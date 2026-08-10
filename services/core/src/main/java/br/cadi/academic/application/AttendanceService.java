package br.cadi.academic.application;

import br.cadi.academic.api.dto.response.*;
import br.cadi.academic.api.dto.request.*;
import br.cadi.academic.domain.entities.*;
import br.cadi.academic.domain.enums.AttendanceStatus;
import br.cadi.academic.infra.repositories.*;
import br.cadi.document.application.AtestadoApprovalService;
import br.cadi.exception.*;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.infra.repositories.CollaboratorRepository;
import br.cadi.common.application.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final LectureRepository lectureRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolRoomScheduleSlotRepository scheduleSlotRepository;
    private final AuditLogService auditLogService;
    private final AtestadoApprovalService atestadoApprovalService;

    @Transactional(readOnly = true)
    public LectureCallResponse startCall(Long schoolroomId, LocalDate date) {
        validateLectureDate(date);
        validateScheduleDay(schoolroomId, date);

        SchoolRoom schoolRoom = schoolRoomRepository.findById(schoolroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada", schoolroomId));

        checkSchoolRoomPermission(schoolRoom);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Optional<Lecture> lectureOpt = lectureRepository
                .findBySchoolroomAndDate(schoolroomId, startOfDay, endOfDay);

        List<Enrollment> enrollments =
                enrollmentRepository.findBySchoolroomIdAndStatusTrue(schoolroomId);

        if (enrollments.isEmpty()) throw new EntityNotFoundException("Nenhum aluno matriculado encontrao");

        List<AttendanceItemResponse> items = enrollments.stream().map(e -> {
            var existing = lectureOpt.flatMap(l ->
                    attendanceRepository.findByLectureIdAndStudentId(l.getId(), e.getStudent().getId())
            );

            return new AttendanceItemResponse(
                    existing.map(Attendance::getId).orElse(null),
                    e.getStudent().getId(),
                    e.getStudent().getName(),
                    existing.map(Attendance::getStatus).orElse(null),
                    existing.map(Attendance::getObservation).orElse(null)
            );
        }).toList();

        return new LectureCallResponse(
                lectureOpt.map(Lecture::getId).orElse(null),
                lectureOpt.map(Lecture::getTopic).orElse(null),
                lectureOpt.map(Lecture::getDateTime).orElse(null),
                lectureOpt.map(Lecture::getClosed).orElse(false),
                items,
                lectureOpt.map(l -> !attendanceRepository.findByLectureId(l.getId()).isEmpty()).orElse(false)
        );
    }

    private Optional<LocalDateTime> findLastCallChange(Long lectureId) {
        Optional<LocalDateTime> lectureChange = auditLogService.findLastEntityChange("lecture", lectureId);
        Optional<LocalDateTime> attendanceChange = auditLogService.findLastLectureAttendanceUpdate(lectureId);

        return Stream.of(lectureChange, attendanceChange)
                .flatMap(Optional::stream)
                .max(LocalDateTime::compareTo);
    }


    @Transactional
    public void registerBatch(BatchAttendanceRequest request) {
        validateLectureDate(request.date());
        validateScheduleDay(request.schoolroomId(), request.date());

        SchoolRoom schoolRoom = schoolRoomRepository.findById(request.schoolroomId())
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada: "));

        LocalDateTime startOfDay = request.date().atStartOfDay();
        LocalDateTime endOfDay = request.date().atTime(23, 59, 59);

        Lecture lecture = lectureRepository
                .findBySchoolroomAndDate(request.schoolroomId(), startOfDay, endOfDay)
                .orElseGet(() -> lectureRepository.save(
                        Lecture.builder()
                                .schoolroom(schoolRoom)
                                .dateTime(startOfDay)
                                .topic(request.topic() != null ? request.topic() : "Aula do dia")
                                .closed(false)
                                .build()
                ));
        checkPermission(lecture);
        if (lecture.getClosed()) throw new BusinessRuleException("Esta chamada já foi fechada e não pode ser alterada");
        checkNoDuplicateLectureOnDay(lecture);

        List<Enrollment> enrollments =
                enrollmentRepository.findBySchoolroomIdAndStatusTrue(
                        lecture.getSchoolroom().getId()
                );

        for (AttendanceItemRequest item : request.attendances()) {
            boolean isEnrolled = enrollments.stream()
                    .anyMatch(e -> e.getStudent().getId().equals(item.studentId()));

            if (!isEnrolled) throw new BusinessRuleException(
                    "Aluno " + item.studentId() + " não está matriculado nesta turma."
            );

            var existing = attendanceRepository
                    .findByLectureIdAndStudentId(lecture.getId(), item.studentId());

            if (existing.isPresent()) {
                Attendance attendance = existing.get();
                applyStatusWithAtestadoCheck(attendance, item.status(), request.date());
                attendance.setObservation(item.observation());
                attendanceRepository.save(attendance);
            } else {
                var student = enrollments.stream()
                        .filter(e -> e.getStudent().getId().equals(item.studentId()))
                        .findFirst().get().getStudent();

                Attendance newAttendance = Attendance.builder()
                        .lecture(lecture)
                        .student(student)
                        .observation(item.observation())
                        .build();
                applyStatusWithAtestadoCheck(newAttendance, item.status(), request.date());
                attendanceRepository.save(newAttendance);
            }
        }
    }

    /**
     * Aplica o status no registro de presenca, mas se for ABSENT verifica se ha
     * atestado APROVADO cobrindo a data do aluno — neste caso, sobrescreve para
     * JUSTIFIED e grava o vinculo com o documento (permite rollback futuro).
     *
     * <p>Quando o status final nao for JUSTIFIED, limpa o vinculo eventualmente
     * existente para evitar registros orfaos.</p>
     */
    private void applyStatusWithAtestadoCheck(Attendance attendance,
                                              AttendanceStatus requestedStatus,
                                              LocalDate lectureDate) {
        AttendanceStatus finalStatus = requestedStatus;
        Long justifyingDocId = null;

        if (requestedStatus == AttendanceStatus.ABSENT) {
            justifyingDocId = atestadoApprovalService.findCoveringApprovedAtestado(
                    attendance.getStudent().getId(), lectureDate);
            if (justifyingDocId != null) {
                finalStatus = AttendanceStatus.JUSTIFIED;
            }
        }

        attendance.applyStatus(finalStatus);
        attendance.setJustifiedByDocumentId(
                finalStatus == AttendanceStatus.JUSTIFIED ? justifyingDocId : null
        );
    }

    @Transactional
    public void closeCall(Long lectureId) {
        Lecture lecture = findLecture(lectureId);
        checkPermission(lecture);
        if (lecture.getClosed()) throw new BusinessRuleException("Esta chamada já está fechada.");
        lecture.setClosed(true);
        lectureRepository.save(lecture);
    }

    // --- helpers ---
    private Lecture findLecture(Long id) {
        return lectureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture", id));
    }

    private void checkPermission(Lecture lecture) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isManagement = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SECRETARY"));
        if (isManagement) return;

        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROFESSOR"));
        if (!isTeacher) throw new ForbiddenException("Você não tem permissão");

        // Professor só acessa suas próprias turmas
        String email = auth.getName();
        Collaborator teacher = collaboratorRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Você não tem permissão"));

        boolean owns = lectureRepository.isTeacherOfLecture(lecture.getId(), teacher.getId());
        if (!owns) throw new ForbiddenException("Você não tem permissão para registrar chamada desta turma.");
    }

    private void checkManagementOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = auth.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SECRETARY") ||
                        a.getAuthority().equals("ROLE_MANAGER")

                );
        if (!allowed) throw new ForbiddenException("Você não tem permissão");;
    }

    @Transactional
    public void reopenCall(Long lectureId) {
        Lecture lecture = findLecture(lectureId);
        checkManagementOnly();
        if (!lecture.getClosed()) throw new BusinessRuleException("A aula ainda está aberta");

        lecture.setClosed(false);
        lectureRepository.save(lecture);
    }

    private void checkNoDuplicateLectureOnDay(Lecture lecture) {
        boolean duplicate = lectureRepository.existsAnotherLectureOnSameDay(
                lecture.getSchoolroom().getId(),
                lecture.getDateTime(),
                lecture.getId()
        );
        if (duplicate) throw new BusinessException("Já existe um registro de chamada desta esta turma neste dia.", HttpStatus.CONFLICT);
    }
    private void validateLectureDate(LocalDate date) {
        LocalDate today = LocalDate.now();

        if (date.isAfter(today)) {
            throw new BusinessRuleException("Não é permitido registrar chamada para datas futuras.");
        }

        if (date.isBefore(today.minusDays(35))) {
            throw new BusinessRuleException(
                    "Não é permitido registrar chamada para datas anteriores a 35 dias."
            );
        }
    }

    private void checkSchoolRoomPermission(SchoolRoom schoolRoom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isManagement = auth.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_ADMIN") ||
                                a.getAuthority().equals("ROLE_SECRETARY") ||
                                a.getAuthority().equals("ROLE_MANAGER")
                );
        if (isManagement) return;

        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROFESSOR"));
        if (!isTeacher) throw new ForbiddenException("Você não tem permissão");

        // Professor só acessa suas próprias turmas
        String email = auth.getName();
        Collaborator teacher = collaboratorRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Você não tem permissão"));

        if (schoolRoom.getTeacher() == null ||
                !schoolRoom.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("Você não tem permissão para visualizar esta turma.");
        }
    }

    private void validateScheduleDay(Long schoolRoomId, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean hasClass = scheduleSlotRepository
                .existsBySchoolroomIdAndDayOfWeek(schoolRoomId, dayOfWeek);
        if (!hasClass) {
            String dayName = date.format(
                    java.time.format.DateTimeFormatter
                            .ofPattern("EEEE", new java.util.Locale   ("pt", "BR"))
            );
            throw new BusinessRuleException("Esta turma não possui aula na " + dayOfWeek + ". Verifique a grade horária.");
        }
    }
}
