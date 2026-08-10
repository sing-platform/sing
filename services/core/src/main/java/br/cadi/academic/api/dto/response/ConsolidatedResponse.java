package br.cadi.academic.api.dto.response;

import java.time.LocalDate;

public record ConsolidatedResponse (
        LocalDate startDate,
        LocalDate endDate,
        Long schoolRoomId,
        String schoolRoomName,
        Long studentId,
        String studentName,
        ScheduleBlockResponse schedule,
        AttendanceBlockResponse attendance,
        Integer   pendingLectures
) {}

