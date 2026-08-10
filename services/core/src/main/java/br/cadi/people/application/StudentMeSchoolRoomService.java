package br.cadi.people.application;

import br.cadi.academic.api.dto.response.SchoolRoomResponse;
import br.cadi.academic.application.SchoolRoomService;
import br.cadi.academic.domain.entities.Enrollment;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.exception.BusinessException;
import br.cadi.iam.application.AuthService;
import br.cadi.iam.domain.User;
import br.cadi.people.domain.entities.Guardian;
import br.cadi.people.domain.entities.Person;
import br.cadi.people.domain.entities.Student;
import br.cadi.people.domain.entities.StudentGuardian;
import br.cadi.people.infra.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentMeSchoolRoomService {

    private final AuthService authService;
    private final PersonRepository personRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentGuardianService studentGuardianService;
    private final SchoolRoomService schoolRoomService;

    @Transactional(readOnly = true)
    public List<SchoolRoomResponse> listMyEnrolledSchoolRooms() {
        User user = authService.getPrincipal();
        Person person = personRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        if (person instanceof Student student) {
            List<Enrollment> enrollments = enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(student.getId());
            List<Long> ids = enrollments.stream()
                    .map(Enrollment::getSchoolroom)
                    .filter(Objects::nonNull)
                    .map(SchoolRoom::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return schoolRoomService.listResponsesBySchoolRoomIds(ids);
        }
        if (person instanceof Guardian guardian) {
            Set<Long> roomIds = new LinkedHashSet<>();
            List<StudentGuardian> tree = studentGuardianService.getFamilyTree(guardian.getId());
            for (StudentGuardian sg : tree) {
                Student s = sg.getStudent();
                if (s == null || s.getEnrollments() == null) {
                    continue;
                }
                for (Enrollment e : s.getEnrollments()) {
                    if (!Boolean.TRUE.equals(e.getStatus()) || e.getSchoolroom() == null) {
                        continue;
                    }
                    roomIds.add(e.getSchoolroom().getId());
                }
            }
            return schoolRoomService.listResponsesBySchoolRoomIds(new ArrayList<>(roomIds));
        }
        throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
    }
}
