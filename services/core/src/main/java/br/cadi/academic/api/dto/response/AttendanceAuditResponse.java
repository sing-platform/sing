package br.cadi.academic.api.dto.response;

import java.time.LocalDateTime;

public record AttendanceAuditResponse(
        Long attendanceId,
        String action,
        Long changedBy,
        LocalDateTime changedAt
) {
}