package br.cadi.finance.api.dto.request;

import br.cadi.finance.domain.EntryDirection;
import br.cadi.finance.domain.EntryStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialEntryCreateRequest(
        @NotNull EntryDirection direction,
        @NotNull EntryStatus status,
        @NotNull LocalDate dueDate,
        LocalDate paidAt,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String category,
        String description,
        Long sponsorId,
        Long fiscalDocumentId
) {
}
