package br.sing.academic.api.dto.response;

import java.util.List;

public record StudentAttendanceSummaryResponse(
        Long studentId,
        String studentName,
        int totalClasses,
        int totalPresent,      // PRESENT
        int totalAbsent,       // ABSENT
        int totalJustified,    // JUSTIFIED
        double attendanceRate, // % calculado no backend
        List<AttendanceRecordResponse> records
) {
}
