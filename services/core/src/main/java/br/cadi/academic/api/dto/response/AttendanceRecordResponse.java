package br.cadi.academic.api.dto.response;

import br.cadi.academic.domain.enums.AttendanceStatus;
import java.time.LocalDate;

public record AttendanceRecordResponse(
        LocalDate date,
        String topic,
        AttendanceStatus status,
        String observation
) {}
