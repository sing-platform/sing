package br.sing.academic.application;

import br.sing.academic.api.dto.response.*;
import br.sing.academic.domain.entities.Attendance;
import br.sing.academic.domain.entities.SchoolRoomScheduleSlot;
import br.sing.academic.domain.enums.AttendanceStatus;
import br.sing.academic.infra.repositories.AttendanceRepository;
import br.sing.academic.infra.repositories.LectureRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.academic.infra.repositories.SchoolRoomScheduleSlotRepository;
import br.sing.common.application.AuditLogService;
import br.sing.exception.ForbiddenException;
import br.sing.people.domain.entities.Person;
import br.sing.people.domain.entities.Student;
import br.sing.people.infra.repositories.PersonRepository;
import br.sing.people.application.StudentGuardianService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidatedQueryService {

    private final AttendanceRepository attendanceRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final ScheduleQueryService scheduleQueryService;
    private final PersonRepository personRepository;
    private final StudentGuardianService studentGuardianService;
    private final AuditLogService auditLogService;
    private final LectureRepository lectureRepository;
    private final SchoolRoomScheduleSlotRepository scheduleSlotRepository;

    @Transactional(readOnly = true)
    public ConsolidatedResponse getConsolidated(
            Long schoolRoomId,
            Long studentId,
            LocalDate startDate,
            LocalDate endDate,
            String turno,
            Authentication auth
    ) {
        LocalDate resolvedStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate resolvedEnd   = endDate   != null ? endDate   : LocalDate.now();

        checkScopePermission(auth, schoolRoomId, studentId);

        String schoolRoomName = null;
        if (schoolRoomId != null) {
            schoolRoomName = schoolRoomRepository.findById(schoolRoomId)
                    .map(r -> r.getName())
                    .orElse(null);
        }

        String studentName = null;
        if (studentId != null) {
            studentName = personRepository.findById(studentId)
                    .map(Person::getName)
                    .orElse(null);
        }

        // ── Bloco de grade (resiliente) ──────────────────────────────────────
        ScheduleBlockResponse scheduleBlock = buildScheduleBlock(
                schoolRoomId, studentId, turno, auth
        );

        // ── Bloco de frequência (resiliente) ─────────────────────────────────
        AttendanceBlockResponse attendanceBlock = buildAttendanceBlock(
                schoolRoomId, studentId, resolvedStart, resolvedEnd
        );

        Integer pendingLectures = null;
        if (schoolRoomId != null) {
            pendingLectures = calcularPendencias(schoolRoomId, resolvedStart, resolvedEnd);
        }

        return new ConsolidatedResponse(
                resolvedStart,
                resolvedEnd,
                schoolRoomId,
                schoolRoomName,
                studentId,
                studentName,
                scheduleBlock,
                attendanceBlock,
                pendingLectures
        );
    }

    // ── Grade ────────────────────────────────────────────────────────────────

    private ScheduleBlockResponse buildScheduleBlock(
            Long schoolRoomId, Long studentId, String turno, Authentication auth
    ) {
        try {
            var items = scheduleQueryService.getSchedule(
                    schoolRoomId, null, studentId, turno, auth
            );
            if (items.isEmpty()) {
                return new ScheduleBlockResponse(false, "Nenhum horário cadastrado para os filtros informados.", List.of());
            }
            return new ScheduleBlockResponse(true, null, items);
        } catch (Exception e) {
            log.warn("Erro ao buscar grade horária no consolidado: {}", e.getMessage());
            return new ScheduleBlockResponse(false, "Grade horária temporariamente indisponível.", List.of());
        }
    }

    // ── Frequência ───────────────────────────────────────────────────────────

    private AttendanceBlockResponse buildAttendanceBlock(
            Long schoolRoomId, Long studentId,
            LocalDate startDate, LocalDate endDate
    ) {
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end   = endDate.atTime(23, 59, 59);

            List<Attendance> attendances = fetchAttendances(schoolRoomId, studentId, start, end);

            if (attendances.isEmpty()) {
                return new AttendanceBlockResponse(
                        false, "Nenhum registro de frequência encontrado para o período.", List.of()
                );
            }

            List<StudentAttendanceSummaryResponse> summaries = buildSummaries(attendances);

            return new AttendanceBlockResponse(true, null, summaries);

        } catch (Exception e) {
            log.warn("Erro ao buscar frequência no consolidado: {}", e.getMessage());
            return new AttendanceBlockResponse(
                    false, "Frequência temporariamente indisponível.", List.of()
            );
        }
    }

    private List<Attendance> fetchAttendances(
            Long schoolRoomId, Long studentId,
            LocalDateTime start, LocalDateTime end
    ) {
        if (schoolRoomId != null && studentId != null) {
            return attendanceRepository.findByStudentAndSchoolRoomAndPeriod(
                    studentId, schoolRoomId, start, end
            );
        }
        if (schoolRoomId != null) {
            return attendanceRepository.findBySchoolRoomAndPeriod(schoolRoomId, start, end);
        }
        if (studentId != null) {
            return attendanceRepository.findByStudentAndPeriod(studentId, start, end);
        }
        return List.of();
    }

    // ── Cálculo de percentual e agrupamento por aluno ────────────────────────

    private List<StudentAttendanceSummaryResponse> buildSummaries(List<Attendance> attendances) {
        Map<Long, List<Attendance>> byStudent = attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        return byStudent.entrySet().stream()
                .map(entry -> {
                    List<Attendance> studentAttendances = entry.getValue();
                    Student student = studentAttendances.get(0).getStudent();

                    int total     = studentAttendances.size();
                    int present   = countByStatus(studentAttendances, AttendanceStatus.PRESENT);
                    int absent    = countByStatus(studentAttendances, AttendanceStatus.ABSENT);
                    int justified = countByStatus(studentAttendances, AttendanceStatus.JUSTIFIED);

                    double rate = total > 0 ? Math.round((present * 100.0 / total) * 10.0) / 10.0 : 0.0;

                    List<AttendanceRecordResponse> records = studentAttendances.stream()
                            .map(a -> new AttendanceRecordResponse(
                                    a.getLecture().getDateTime().toLocalDate(),
                                    a.getLecture().getTopic(),
                                    a.getStatus(),
                                    a.getObservation()
                            ))
                            .sorted(Comparator.comparing(AttendanceRecordResponse::date))
                            .toList();

                    return new StudentAttendanceSummaryResponse(
                            student.getId(),
                            student.getName(),
                            total,
                            present,
                            absent,
                            justified,
                            rate,
                            records
                    );
                })
                .sorted(Comparator.comparing(StudentAttendanceSummaryResponse::studentName))
                .toList();
    }

    private int countByStatus(List<Attendance> list, AttendanceStatus... statuses) {
        Set<AttendanceStatus> set = Set.of(statuses);
        return (int) list.stream().filter(a -> set.contains(a.getStatus())).count();
    }

    // ── Validação de escopo por perfil ────────────────────────────────────────

    private void checkScopePermission(Authentication auth, Long schoolRoomId, Long studentId) {
        if (auth == null || !auth.isAuthenticated()) throw new ForbiddenException("Não autenticado.");

        boolean isManagement = hasAnyRole(auth, "ROLE_ADMIN", "ROLE_SECRETARY", "ROLE_MANAGER");
        if (isManagement) return; // gestão acessa tudo

        boolean isProfessor = hasAnyRole(auth, "ROLE_PROFESSOR");
        boolean isStudent   = hasAnyRole(auth, "ROLE_STUDENT");
        boolean isParent    = hasAnyRole(auth, "ROLE_PARENT");

        if (!isProfessor && !isStudent && !isParent) throw new ForbiddenException("Perfil sem permissão para esta consulta.");

        if (isStudent && studentId != null) {
            Person person = personRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ForbiddenException("Usuário não encontrado."));
            if (!person.getId().equals(studentId)) {
                throw new ForbiddenException("Aluno só pode consultar sua própria frequência.");
            }
        }

        if (isParent && studentId != null) {
            Person guardian = personRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ForbiddenException("Usuário não encontrado."));
            if (!studentGuardianService.isGuardianOf(guardian.getId(), studentId)) {
                throw new ForbiddenException("Responsável sem permissão para consultar este aluno.");
            }
        }

        if (schoolRoomId == null && studentId == null) {
            throw new ForbiddenException("Informe ao menos turma ou aluno para a consulta.");
        }
    }

    private boolean hasAnyRole(Authentication auth, String... roles) {
        Set<String> roleSet = Set.of(roles);
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleSet::contains);
    }



    private Integer calcularPendencias(Long roomId, LocalDate start, LocalDate end) {
        // Usa a data de vínculo do professor como limite inferior
        LocalDate assignedDate = auditLogService
                .findFirstTeacherAssignment(roomId)
                .map(LocalDateTime::toLocalDate)
                .orElse(start);

        LocalDate effectiveStart = assignedDate.isAfter(start) ? assignedDate : start;

        List<SchoolRoomScheduleSlot> slots = scheduleSlotRepository.findBySchoolroomId(roomId);
        int expected   = countExpectedLectures(slots, effectiveStart, end);
        long registered = lectureRepository.countRegisteredLectures(
                roomId,
                effectiveStart.atStartOfDay(),
                end.atTime(23, 59, 59)
        );

        return Math.max(0, expected - (int) registered);
    }

    private int countExpectedLectures(List<SchoolRoomScheduleSlot> slots, LocalDate start, LocalDate end) {
        Set<DayOfWeek> classDays = slots.stream()
                .map(SchoolRoomScheduleSlot::getDayOfWeek)
                .collect(Collectors.toSet());
        if (classDays.isEmpty()) return 0;
        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (classDays.contains(current.getDayOfWeek())) count++;
            current = current.plusDays(1);
        }
        return count;
    }
}