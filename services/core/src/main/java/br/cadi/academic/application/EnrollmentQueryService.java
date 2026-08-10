package br.cadi.academic.application;

import br.cadi.academic.api.dto.response.EnrollmentLinkResponse;
import br.cadi.academic.domain.entities.Enrollment;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.entities.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final AcademicAuthorizationService academicAuthorizationService;

    @Transactional(readOnly = true)
    public Page<EnrollmentLinkResponse> pageBySchoolRoom(
            Long schoolRoomId,
            Boolean active,
            Pageable pageable,
            Authentication authentication
    ) {
        SchoolRoom schoolRoom = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(schoolRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada"));
        academicAuthorizationService.assertCanAccessSchoolRoomAsProfessor(authentication, schoolRoom);
        Pageable p = AcademicPageables.normalizeEnrollmentPageable(pageable);
        return enrollmentRepository
                .pageBySchoolRoomIdAndStatusOptional(schoolRoomId, active, p)
                .map(this::toLink);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentLinkResponse> pageByStudent(
            Long studentId,
            Boolean active,
            Pageable pageable,
            Authentication authentication
    ) {
        academicAuthorizationService.assertAuthenticated(authentication);
        Pageable p = AcademicPageables.normalizeEnrollmentPageable(pageable);
        if (academicAuthorizationService.isManagement(authentication)) {
            return enrollmentRepository.pageByStudentIdAndStatusOptional(studentId, active, p).map(this::toLink);
        }
        if (academicAuthorizationService.isProfessor(authentication)) {
            Collaborator collaborator = academicAuthorizationService.getCollaboratorOrThrow(authentication);
            return enrollmentRepository
                    .pageByStudentIdAndTeacherIdAndStatusOptional(studentId, collaborator.getId(), active, p)
                    .map(this::toLink);
        }
        throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentLinkResponse> pageByTeacher(
            Long teacherId,
            Boolean active,
            Pageable pageable,
            Authentication authentication
    ) {
        academicAuthorizationService.assertProfessorOwnsTeacherIdOrManagement(authentication, teacherId);
        Pageable p = AcademicPageables.normalizeEnrollmentPageable(pageable);
        return enrollmentRepository.pageByTeacherIdAndStatusOptional(teacherId, active, p).map(this::toLink);
    }

    private EnrollmentLinkResponse toLink(Enrollment e) {
        Student student = e.getStudent();
        SchoolRoom room = e.getSchoolroom();
        Collaborator teacher = room.getTeacher();
        String teacherName = teacher != null ? teacher.getName() : null;
        Long teacherId = teacher != null ? teacher.getId() : null;
        return new EnrollmentLinkResponse(
                e.getId(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                room.getId(),
                room.getName(),
                room.getWorkshop().getId(),
                room.getWorkshop().getName(),
                teacherId,
                teacherName,
                e.getEnrollmentDate(),
                Boolean.TRUE.equals(e.getStatus())
        );
    }
}
