package br.cadi.document.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Periodo coberto pelo atestado. Para 1 dia, basta start = end.
 * Default sugerido pela UI: ambas = data do upload.
 */
public record AtestadoApproveRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
