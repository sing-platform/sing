package br.cadi.academic.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record LectureCallResponse(
        Long lectureId,
        String topic,
        LocalDateTime dateTime,
        Boolean closed,
        List<AttendanceItemResponse> attendances,
        Boolean   hasAttendance
) {}
