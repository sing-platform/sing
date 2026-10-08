package br.sing.finance.api.dto.request;

import br.sing.finance.domain.FiscalDocumentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FiscalDocumentUpdateRequest(
        @NotNull FiscalDocumentType documentType,
        @NotBlank String documentNumber,
        @NotNull LocalDate issueDate,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        String supplierName,
        String supplierDoc,
        String fileUrl
) {
}
