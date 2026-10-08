package br.sing.academic.api.dto.response;
import br.sing.academic.domain.enums.AttendanceStatus;

public record AttendanceItemResponse(
        Long attendanceId,
        Long studentId,
        String studentName,
        AttendanceStatus status,
        String observation
) {}
