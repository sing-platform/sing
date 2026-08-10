package br.cadi.finance.api.dto.response;

import br.cadi.finance.domain.FiscalDocument;
import br.cadi.finance.domain.FiscalDocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FiscalDocumentResponse(
        Long id,
        FiscalDocumentType documentType,
        String documentNumber,
        LocalDate issueDate,
        BigDecimal totalAmount,
        String supplierName,
        String supplierDoc,
        String fileUrl,
        boolean active
) {
    public static FiscalDocumentResponse from(FiscalDocument document) {
        return new FiscalDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getIssueDate(),
                document.getTotalAmount(),
                document.getSupplierName(),
                document.getSupplierDoc(),
                document.getFileUrl(),
                document.isActive()
        );
    }
}
