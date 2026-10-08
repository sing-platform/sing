package br.sing.finance.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MarkEntryAsPaidRequest(
        @NotNull LocalDate paidAt
) {
}
