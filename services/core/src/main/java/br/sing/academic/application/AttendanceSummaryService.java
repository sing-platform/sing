package br.sing.academic.application;

import br.sing.academic.api.dto.response.LectureSummaryResponse;
import br.sing.academic.api.dto.response.SchoolRoomAttendanceSummaryResponse;
import br.sing.academic.domain.entities.Attendance;
import br.sing.academic.domain.entities.Lecture;
import br.sing.academic.domain.enums.AttendanceStatus;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.SchoolRoomScheduleSlot;
import br.sing.academic.infra.repositories.*;
import br.sing.common.application.AuditLogService;
import br.sing.common.infra.AuditLogRepository;
import br.sing.exception.ForbiddenException;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.infra.repositories.CollaboratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceSummaryService {

    private final LectureRepository         lectureRepository;
    private final AttendanceRepository      attendanceRepository;
    private final SchoolRoomRepository      schoolRoomRepository;
    private final CollaboratorRepository    collaboratorRepository;
    private final SchoolRoomScheduleSlotRepository scheduleSlotRepository;
    private final AuditLogService auditLogService;


    @Transactional(readOnly = true)
    public List<SchoolRoomAttendanceSummaryResponse> getMySchoolRoomsSummary(Authentication auth) {
        String email = auth.getName();
        Collaborator teacher = collaboratorRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Professor não encontrado."));

        List<SchoolRoom> rooms = schoolRoomRepository
                .findByTeacherIdAndIsActiveTrue(teacher.getId());

        if (rooms.isEmpty()) return List.of();

        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);

        return rooms.stream()
                .map(room -> buildSummary(room, yearStart, today))
                .sorted(Comparator.comparing(SchoolRoomAttendanceSummaryResponse::schoolRoomName))
                .toList();
    }

    private SchoolRoomAttendanceSummaryResponse buildSummary(
            SchoolRoom room, LocalDate yearStart, LocalDate today
    ) {
        Long roomId = room.getId();

        // Data de início = quando o professor foi vinculado OU início do ano, o que for maior
        LocalDate assignedDate = auditLogService
                .findFirstTeacherAssignment(roomId)
                .map(LocalDateTime::toLocalDate)
                .orElse(yearStart);

        LocalDate effectiveStart = assignedDate.isAfter(yearStart) ? assignedDate : yearStart;

        LocalDateTime start = effectiveStart.atStartOfDay();
        LocalDateTime end   = today.atTime(23, 59, 59);

        List<SchoolRoomScheduleSlot> slots = scheduleSlotRepository.findBySchoolroomId(roomId);

        int expectedLectures = countExpectedLectures(slots, effectiveStart, today);
        long registered      = lectureRepository.countRegisteredLectures(roomId, start, end);
        int pendingLectures  = Math.max(0, expectedLectures - (int) registered);

        double registrationRate = expectedLectures > 0
                ? Math.round((registered * 100.0 / expectedLectures) * 10.0) / 10.0
                : 0.0;

        List<Attendance> attendances = attendanceRepository.findBySchoolRoomAndPeriod(roomId, start, end);
        double avgAttendance = calculateAvgAttendance(attendances);

        long totalStudents = attendances.stream()
                .map(a -> a.getStudent().getId())
                .distinct()
                .count();

        return new SchoolRoomAttendanceSummaryResponse(
                roomId,
                room.getName(),
                room.getClassroom(),
                room.getWorkshop() != null ? room.getWorkshop().getName() : null,
                expectedLectures,
                (int) registered,
                pendingLectures,
                registrationRate,
                (int) totalStudents,
                avgAttendance
        );
    }

    // Conta quantas ocorrências de cada dia de grade caem no período
    private int countExpectedLectures(
            List<SchoolRoomScheduleSlot> slots,
            LocalDate start,
            LocalDate end
    ) {
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

    // Média de presença / total por aluno, depois média geral
    private double calculateAvgAttendance(List<Attendance> attendances) {
        if (attendances.isEmpty()) return 0.0;

        Map<Long, List<Attendance>> byStudent = attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        double avg = byStudent.values().stream()
                .mapToDouble(list -> {
                    long present = list.stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                            .count();
                    return list.isEmpty() ? 0 : (present * 100.0 / list.size());
                })
                .average()
                .orElse(0.0);

        return Math.round(avg * 10.0) / 10.0;
    }
    @Transactional(readOnly = true)
    public List<LectureSummaryResponse> getLecturesSummary(
            Long roomId, LocalDate start, LocalDate end
    ) {
        List<Lecture> lectures = lectureRepository.findBySchoolroomIdAndPeriod(
                roomId,
                start.atStartOfDay(),
                end.atTime(23, 59, 59)
        );

        List<SchoolRoomScheduleSlot> slots = scheduleSlotRepository.findBySchoolroomId(roomId);
        Set<DayOfWeek> classDays = slots.stream()
                .map(SchoolRoomScheduleSlot::getDayOfWeek)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<LocalDate, Lecture> lecturesByDate = lectures.stream()
                .collect(Collectors.toMap(
                        l -> l.getDateTime().toLocalDate(),
                        l -> l,
                        (first, second) -> first
                ));

        List<LectureSummaryResponse> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (classDays.contains(current.getDayOfWeek())) {
                Lecture lecture = lecturesByDate.get(current);
                if (lecture != null) {
                    boolean hasAttendance = !attendanceRepository.findByLectureId(lecture.getId()).isEmpty();
                    result.add(new LectureSummaryResponse(
                            lecture.getId(),
                            lecture.getDateTime().toLocalDate(),
                            lecture.getTopic(),
                            Boolean.TRUE.equals(lecture.getClosed()),
                            hasAttendance
                    ));
                } else {
                    result.add(new LectureSummaryResponse(
                            null,
                            current,
                            null,
                            false,
                            false
                    ));
                }
            }
            current = current.plusDays(1);
        }

        return result;
    }

}