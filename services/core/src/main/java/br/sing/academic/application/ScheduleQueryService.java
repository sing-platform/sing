package br.sing.academic.application;

import br.sing.academic.api.dto.response.ScheduleItemResponse;
import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.SchoolRoomScheduleSlot;
import br.sing.academic.infra.repositories.SchoolRoomScheduleSlotRepository;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.domain.entities.Student;
import br.sing.people.domain.entities.Person;
import br.sing.people.infra.repositories.PersonRepository;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService {

    private final SchoolRoomScheduleSlotRepository scheduleSlotRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public List<ScheduleItemResponse> getSchedule(
            Long schoolRoomId,
            Long teacherId,
            Long studentId,
            String turno,
            Authentication auth
    ) {
        Specification<SchoolRoomScheduleSlot> spec = Specification.where(null);

        spec = spec.and(applySecurityScope(auth));

        if (schoolRoomId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("schoolroom").get("id"), schoolRoomId));
        }

        if (teacherId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("schoolroom").get("teacher").get("id"), teacherId));
        }

        if (studentId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<SchoolRoomScheduleSlot, SchoolRoom> schoolRoomJoin = root.join("schoolroom");
                Join<SchoolRoom, Enrollment> enrollmentJoin = schoolRoomJoin.join("enrollments");
                Join<Enrollment, Student> studentJoin = enrollmentJoin.join("student");
                
                Predicate isStudent = cb.equal(studentJoin.get("id"), studentId);
                Predicate isActiveEnrollment = cb.isTrue(enrollmentJoin.get("status"));
                return cb.and(isStudent, isActiveEnrollment);
            });
        }

        if (turno != null && !turno.trim().isEmpty()) {
            spec = spec.and(applyTurnoFilter(turno));
        }

        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("schoolroom").get("isActive")));

        List<SchoolRoomScheduleSlot> slots = scheduleSlotRepository.findAll(spec);

        return slots.stream()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(ScheduleItemResponse::dayOfWeek).thenComparing(ScheduleItemResponse::startTime))
                .distinct()
                .collect(Collectors.toList());
    }

    private Specification<SchoolRoomScheduleSlot> applySecurityScope(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return (root, query, cb) -> cb.disjunction(); // returns nothing
        }

        boolean isManagement = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SECRETARY"));

        if (isManagement) {
            return (root, query, cb) -> cb.conjunction();
        }

        Optional<Person> personOpt = personRepository.findByEmail(auth.getName());
        if (personOpt.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }

        Person person = personOpt.get();
        Long personId = person.getId();

        boolean isProfessor = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_PROFESSOR"));

        if (isProfessor) {
            return (root, query, cb) -> cb.equal(root.get("schoolroom").get("teacher").get("id"), personId);
        }

        boolean isStudent = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_STUDENT"));

        if (isStudent) {
            return (root, query, cb) -> {
                query.distinct(true);
                Join<SchoolRoomScheduleSlot, SchoolRoom> schoolRoomJoin = root.join("schoolroom");
                Join<SchoolRoom, Enrollment> enrollmentJoin = schoolRoomJoin.join("enrollments");
                Join<Enrollment, Student> studentJoin = enrollmentJoin.join("student");
                
                Predicate isStudentUser = cb.equal(studentJoin.get("id"), personId);
                Predicate isActiveEnrollment = cb.isTrue(enrollmentJoin.get("status"));
                return cb.and(isStudentUser, isActiveEnrollment);
            };
        }

        return (root, query, cb) -> cb.disjunction();
    }

    private Specification<SchoolRoomScheduleSlot> applyTurnoFilter(String turno) {
        return (root, query, cb) -> {
            String t = turno.toUpperCase();
            if (t.equals("MANHA") || t.equals("MATUTINO")) {
                return cb.lessThan(root.get("startTime"), LocalTime.of(12, 0));
            } else if (t.equals("TARDE") || t.equals("VESPERTINO")) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.get("startTime"), LocalTime.of(12, 0)),
                        cb.lessThan(root.get("startTime"), LocalTime.of(18, 0))
                );
            } else if (t.equals("NOITE") || t.equals("NOTURNO")) {
                return cb.greaterThanOrEqualTo(root.get("startTime"), LocalTime.of(18, 0));
            } else {
                return cb.conjunction();
            }
        };
    }

    private ScheduleItemResponse mapToResponse(SchoolRoomScheduleSlot slot) {
        SchoolRoom room = slot.getSchoolroom();
        Collaborator teacher = room.getTeacher();
        return new ScheduleItemResponse(
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                room.getId(),
                room.getName(),
                room.getClassroom(),
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getName() : null
        );
    }
}