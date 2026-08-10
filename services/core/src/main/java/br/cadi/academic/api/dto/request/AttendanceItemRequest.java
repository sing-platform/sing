package br.cadi.academic.api.dto.request;

import br.cadi.academic.domain.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceItemRequest(
        @NotNull Long studentId,
        @NotNull AttendanceStatus status,
        String observation
) {}
