package br.cadi.academic.api.dto.response;

import java.util.List;

public record AttendanceBlockResponse(
        boolean available,
        String unavailableReason,
        List<StudentAttendanceSummaryResponse> students
) {}