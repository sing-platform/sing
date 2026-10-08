package br.sing.finance.api.dto.response;

import br.sing.finance.domain.EntryDirection;
import br.sing.finance.domain.EntryStatus;
import br.sing.finance.domain.FinancialEntry;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialEntryResponse(
        Long id,
        EntryDirection direction,
        EntryStatus status,
        LocalDate dueDate,
        LocalDate paidAt,
        BigDecimal amount,
        String category,
        String description,
        Long sponsorId,
        String sponsorName,
        Long fiscalDocumentId,
        String fiscalDocumentNumber,
        boolean active
) {
    public static FinancialEntryResponse from(FinancialEntry entry) {
        return new FinancialEntryResponse(
                entry.getId(),
                entry.getDirection(),
                entry.getStatus(),
                entry.getDueDate(),
                entry.getPaidAt(),
                entry.getAmount(),
                entry.getCategory(),
                entry.getDescription(),
                entry.getSponsor() != null ? entry.getSponsor().getId() : null,
                entry.getSponsor() != null ? entry.getSponsor().getName() : null,
                entry.getFiscalDocument() != null ? entry.getFiscalDocument().getId() : null,
                entry.getFiscalDocument() != null ? entry.getFiscalDocument().getDocumentNumber() : null,
                entry.isActive()
        );
    }
}
