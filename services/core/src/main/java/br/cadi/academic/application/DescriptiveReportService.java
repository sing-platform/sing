package br.cadi.academic.application;

import br.cadi.academic.api.dto.request.DescriptiveReportUpsertRequest;
import br.cadi.academic.api.dto.response.DescriptiveReportResponse;
import br.cadi.academic.domain.entities.*;
import br.cadi.academic.domain.enums.DescriptiveReportHistoryAction;
import br.cadi.academic.domain.enums.DescriptiveReportStatus;
import br.cadi.academic.infra.repositories.*;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.entities.Student;
import br.cadi.people.infra.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DescriptiveReportService {

    private final DescriptiveReportRepository descriptiveReportRepository;
    private final DescriptiveReportHistoryRepository descriptiveReportHistoryRepository;
    private final StudentRepository studentRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicAuthorizationService academicAuthorizationService;

    @Transactional(readOnly = true)
    public List<DescriptiveReportResponse> list(
            Long schoolRoomId,
            Long studentId,
            DescriptiveReportStatus status,
            Long workshopId,
            Authentication authentication
    ) {
        academicAuthorizationService.assertAuthenticated(authentication);

        List<DescriptiveReport> reports;
        if (academicAuthorizationService.isProfessor(authentication)
                && !academicAuthorizationService.isManagement(authentication)) {
            Long teacherId = academicAuthorizationService.getCollaboratorOrThrow(authentication).getId();
            reports = descriptiveReportRepository.findAllWithFiltersForTeacher(
                    teacherId, schoolRoomId, studentId, status, workshopId
            );
        } else {
            reports = descriptiveReportRepository.findAllWithFilters(
                    schoolRoomId, studentId, status, workshopId
            );
        }
        return reports.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DescriptiveReportResponse getById(Long id, Authentication authentication) {
        DescriptiveReport report = getReportOrThrow(id);
        assertReadScope(authentication, report);
        return toResponse(report);
    }

    @Transactional
    public DescriptiveReportResponse create(DescriptiveReportUpsertRequest request, Authentication authentication) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado"));
        SchoolRoom room = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(request.schoolRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada"));

        assertWriteScope(authentication, room, null);
        assertActiveEnrollment(student.getId(), room.getId());

        if (descriptiveReportRepository.existsByStudent_IdAndSchoolRoom_Id(student.getId(), room.getId())) {
            throw new BusinessException(
                    "Já existe parecer para este aluno nesta turma.",
                    HttpStatus.CONFLICT
            );
        }

        Collaborator actor = academicAuthorizationService.getCollaboratorOrThrow(authentication);
        LocalDateTime now = LocalDateTime.now();

        DescriptiveReport created = descriptiveReportRepository.save(
                DescriptiveReport.builder()
                        .student(student)
                        .schoolRoom(room)
                        .author(actor)
                        .content(request.content().trim())
                        .status(DescriptiveReportStatus.DRAFT)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        addHistory(created, actor, DescriptiveReportHistoryAction.CREATED, null);
        return toResponse(created);
    }

    @Transactional
    public DescriptiveReportResponse update(Long id, DescriptiveReportUpsertRequest request, Authentication authentication) {
        DescriptiveReport report = getReportOrThrow(id);
        assertWriteScope(authentication, report.getSchoolRoom(), report);
        assertEditable(report);

        if (!report.getStudent().getId().equals(request.studentId())
                || !report.getSchoolRoom().getId().equals(request.schoolRoomId())) {
            throw new BusinessException(
                    "Não é permitido alterar aluno ou turma no parecer existente.",
                    HttpStatus.BAD_REQUEST
            );
        }

        String previous = report.getContent();
        report.setContent(request.content().trim());
        report.setUpdatedAt(LocalDateTime.now());
        DescriptiveReport saved = descriptiveReportRepository.save(report);

        addHistory(saved, academicAuthorizationService.getCollaboratorOrThrow(authentication),
                DescriptiveReportHistoryAction.UPDATED, previous);
        return toResponse(saved);
    }

    @Transactional
    public DescriptiveReportResponse publish(Long id, Authentication authentication) {
        DescriptiveReport report = getReportOrThrow(id);
        assertWriteScope(authentication, report.getSchoolRoom(), report);

        if (report.getStatus() == DescriptiveReportStatus.CLOSED) {
            throw new BusinessException("Parecer fechado não pode ser publicado.", HttpStatus.CONFLICT);
        }
        if (report.getStatus() == DescriptiveReportStatus.PUBLISHED) {
            throw new BusinessException("Parecer já está publicado.", HttpStatus.CONFLICT);
        }

        report.setStatus(DescriptiveReportStatus.PUBLISHED);
        report.setPublishedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        DescriptiveReport saved = descriptiveReportRepository.save(report);
        addHistory(saved, academicAuthorizationService.getCollaboratorOrThrow(authentication),
                DescriptiveReportHistoryAction.PUBLISHED, null);
        return toResponse(saved);
    }

    @Transactional
    public DescriptiveReportResponse close(Long id, Authentication authentication) {
        DescriptiveReport report = getReportOrThrow(id);
        assertWriteScope(authentication, report.getSchoolRoom(), report);

        if (report.getStatus() == DescriptiveReportStatus.CLOSED) {
            throw new BusinessException("Parecer já está fechado.", HttpStatus.CONFLICT);
        }

        report.setStatus(DescriptiveReportStatus.CLOSED);
        report.setUpdatedAt(LocalDateTime.now());
        DescriptiveReport saved = descriptiveReportRepository.save(report);
        addHistory(saved, academicAuthorizationService.getCollaboratorOrThrow(authentication),
                DescriptiveReportHistoryAction.CLOSED, null);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        DescriptiveReport report = getReportOrThrow(id);
        assertWriteScope(authentication, report.getSchoolRoom(), report);
        assertEditable(report);
        descriptiveReportRepository.delete(report);
    }

    private DescriptiveReport getReportOrThrow(Long id) {
        return descriptiveReportRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Parecer não encontrado"));
    }

    private void assertReadScope(Authentication authentication, DescriptiveReport report) {
        academicAuthorizationService.assertAuthenticated(authentication);
        if (academicAuthorizationService.isManagement(authentication)) {
            return;
        }
        academicAuthorizationService.assertCanAccessSchoolRoomAsProfessor(authentication, report.getSchoolRoom());
    }

    private void assertWriteScope(Authentication authentication, SchoolRoom room, DescriptiveReport report) {
        academicAuthorizationService.assertAuthenticated(authentication);
        if (academicAuthorizationService.isManagement(authentication)) {
            return;
        }
        academicAuthorizationService.assertCanAccessSchoolRoomAsProfessor(authentication, room);
        if (report != null) {
            Long actorId = academicAuthorizationService.getCollaboratorOrThrow(authentication).getId();
            if (!report.getAuthor().getId().equals(actorId)) {
                throw new BusinessException("Professor só pode alterar o próprio parecer.", HttpStatus.FORBIDDEN);
            }
        }
    }

    private void assertEditable(DescriptiveReport report) {
        if (report.getStatus() != DescriptiveReportStatus.DRAFT) {
            throw new BusinessException(
                    "Parecer publicado ou fechado não pode ser editado/excluído.",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void assertActiveEnrollment(Long studentId, Long schoolRoomId) {
        if (!enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(studentId, schoolRoomId)) {
            throw new BusinessException(
                    "Aluno não possui matrícula ativa na turma informada.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void addHistory(
            DescriptiveReport report,
            Collaborator actor,
            DescriptiveReportHistoryAction action,
            String previousContent
    ) {
        descriptiveReportHistoryRepository.save(
                DescriptiveReportHistory.builder()
                        .descriptiveReport(report)
                        .actor(actor)
                        .action(action)
                        .previousContent(previousContent)
                        .occurredAt(LocalDateTime.now())
                        .build()
        );
    }

    private DescriptiveReportResponse toResponse(DescriptiveReport report) {
        return new DescriptiveReportResponse(
                report.getId(),
                report.getStudent().getId(),
                report.getStudent().getName(),
                report.getSchoolRoom().getId(),
                report.getSchoolRoom().getName(),
                report.getSchoolRoom().getWorkshop().getId(),
                report.getSchoolRoom().getWorkshop().getName(),
                report.getAuthor().getId(),
                report.getAuthor().getName(),
                report.getContent(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getPublishedAt()
        );
    }
}
