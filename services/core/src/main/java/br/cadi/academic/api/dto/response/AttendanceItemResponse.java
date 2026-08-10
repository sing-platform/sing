package br.cadi.academic.api.dto.response;
import br.cadi.academic.domain.enums.AttendanceStatus;

public record AttendanceItemResponse(
        Long attendanceId,
        Long studentId,
        String studentName,
        AttendanceStatus status,
        String observation
) {}
