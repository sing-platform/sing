package br.sing.finance.api.dto.request;

import br.sing.finance.domain.SponsorFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SponsorUpdateRequest(
        @NotNull Long studentId,
        @NotBlank String name,
        String email,
        String phone,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull SponsorFrequency frequency,
        @NotNull LocalDate startDate,
        @NotNull Boolean active
) {
}
