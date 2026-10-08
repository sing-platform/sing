package br.sing.academic.api.dto.response;

import java.time.LocalDate;

public record LectureSummaryResponse(
        Long      lectureId,
        LocalDate date,
        String    topic,
        boolean   closed,
        boolean   hasAttendance
) {}