package br.cadi.finance.application;

import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.finance.api.dto.request.FinancialEntryCreateRequest;
import br.cadi.finance.api.dto.request.FinancialEntryUpdateRequest;
import br.cadi.finance.api.dto.response.FinancialEntryResponse;
import br.cadi.finance.api.dto.response.FinancialSummaryResponse;
import br.cadi.finance.domain.*;
import br.cadi.finance.infra.repositories.FinancialEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialEntryService {

    private final FinancialEntryRepository financialEntryRepository;
    private final SponsorService sponsorService;
    private final FiscalDocumentService fiscalDocumentService;

    @Transactional(readOnly = true)
    public List<FinancialEntryResponse> list(
            YearMonth month,
            EntryDirection direction,
            EntryStatus status,
            String category
    ) {
        LocalDate start = month != null ? month.atDay(1) : null;
        LocalDate finish = month != null ? month.atEndOfMonth() : null;
        return financialEntryRepository.search(start, finish, direction, status, category)
                .stream()
                .map(FinancialEntryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FinancialEntryResponse getById(Long id) {
        return FinancialEntryResponse.from(findOrThrow(id));
    }

    @Transactional
    public FinancialEntryResponse create(FinancialEntryCreateRequest request) {
        FinancialEntry entry = FinancialEntry.builder()
                .direction(request.direction())
                .status(request.status())
                .dueDate(request.dueDate())
                .paidAt(request.paidAt())
                .amount(request.amount())
                .category(request.category().trim())
                .description(request.description())
                .sponsor(resolveSponsor(request.sponsorId()))
                .fiscalDocument(fiscalDocumentService.findEntityOrNull(request.fiscalDocumentId()))
                .build();
        validatePaid(entry.getStatus(), entry.getPaidAt());
        return FinancialEntryResponse.from(financialEntryRepository.save(entry));
    }

    @Transactional
    public FinancialEntryResponse update(Long id, FinancialEntryUpdateRequest request) {
        FinancialEntry entry = findOrThrow(id);
        entry.setDirection(request.direction());
        entry.setStatus(request.status());
        entry.setDueDate(request.dueDate());
        entry.setPaidAt(request.paidAt());
        entry.setAmount(request.amount());
        entry.setCategory(request.category().trim());
        entry.setDescription(request.description());
        entry.setSponsor(resolveSponsor(request.sponsorId()));
        entry.setFiscalDocument(fiscalDocumentService.findEntityOrNull(request.fiscalDocumentId()));
        entry.setActive(request.active());
        validatePaid(entry.getStatus(), entry.getPaidAt());
        return FinancialEntryResponse.from(financialEntryRepository.save(entry));
    }

    @Transactional
    public FinancialEntryResponse markAsPaid(Long id, LocalDate paidAt) {
        FinancialEntry entry = findOrThrow(id);
        entry.setStatus(EntryStatus.PAID);
        entry.setPaidAt(paidAt);
        return FinancialEntryResponse.from(financialEntryRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        FinancialEntry entry = findOrThrow(id);
        entry.setActive(false);
        entry.setDeactivatedAt(java.time.LocalDateTime.now());
        financialEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse monthSummary(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate finish = month.atEndOfMonth();
        List<FinancialEntry> entries = financialEntryRepository.search(start, finish, null, null, null);

        BigDecimal income = sum(entries, EntryDirection.RECEIVABLE, null);
        BigDecimal expense = sum(entries, EntryDirection.PAYABLE, null);
        BigDecimal pendingReceivable = sum(entries, EntryDirection.RECEIVABLE, EntryStatus.PENDING);
        BigDecimal pendingPayable = sum(entries, EntryDirection.PAYABLE, EntryStatus.PENDING);
        return new FinancialSummaryResponse(
                income,
                expense,
                income.subtract(expense),
                pendingReceivable,
                pendingPayable
        );
    }

    @Transactional(readOnly = true)
    public List<FinancialEntryResponse> listBySponsor(Long sponsorId) {
        return financialEntryRepository.findBySponsorIdOrderByDueDateDesc(sponsorId)
                .stream()
                .map(FinancialEntryResponse::from)
                .toList();
    }

    @Transactional
    public FinancialEntry createForSponsorBilling(Sponsor sponsor, LocalDate dueDate) {
        FinancialEntry entry = FinancialEntry.builder()
                .direction(EntryDirection.RECEIVABLE)
                .status(EntryStatus.PENDING)
                .dueDate(dueDate)
                .amount(sponsor.getAmount())
                .category("Padrinho")
                .description("Cobrança automática de padrinho: " + sponsor.getName())
                .sponsor(sponsor)
                .build();
        return financialEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public boolean existsSponsorDueDate(Long sponsorId, LocalDate dueDate) {
        return financialEntryRepository.existsBySponsorIdAndDueDateAndIsActiveTrue(sponsorId, dueDate);
    }

    private BigDecimal sum(List<FinancialEntry> entries, EntryDirection direction, EntryStatus status) {
        return entries.stream()
                .filter(e -> e.getDirection() == direction)
                .filter(e -> status == null || e.getStatus() == status)
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Sponsor resolveSponsor(Long sponsorId) {
        if (sponsorId == null) {
            return null;
        }
        return sponsorService.findEntityOrThrow(sponsorId);
    }

    private void validatePaid(EntryStatus status, LocalDate paidAt) {
        if (status == EntryStatus.PAID && paidAt == null) {
            throw new BusinessException("Data de pagamento é obrigatória para status PAID", HttpStatus.BAD_REQUEST);
        }
    }

    private FinancialEntry findOrThrow(Long id) {
        return financialEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento financeiro não encontrado"));
    }
}
