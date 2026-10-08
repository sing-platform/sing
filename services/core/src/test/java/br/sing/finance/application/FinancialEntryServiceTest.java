package br.sing.finance.application;

import br.sing.finance.api.dto.response.FinancialSummaryResponse;
import br.sing.finance.domain.EntryDirection;
import br.sing.finance.domain.EntryStatus;
import br.sing.finance.domain.FinancialEntry;
import br.sing.finance.infra.repositories.FinancialEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialEntryServiceTest {

    @Mock
    private FinancialEntryRepository financialEntryRepository;
    @Mock
    private SponsorService sponsorService;
    @Mock
    private FiscalDocumentService fiscalDocumentService;

    private FinancialEntryService financialEntryService;

    @BeforeEach
    void setUp() {
        financialEntryService = new FinancialEntryService(
                financialEntryRepository,
                sponsorService,
                fiscalDocumentService
        );
    }

    @Test
    void markAsPaidDeveAtualizarStatusEData() {
        FinancialEntry entry = FinancialEntry.builder()
                .id(1L)
                .direction(EntryDirection.RECEIVABLE)
                .status(EntryStatus.PENDING)
                .dueDate(LocalDate.now())
                .amount(new BigDecimal("10.00"))
                .category("Teste")
                .build();
        when(financialEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(financialEntryRepository.save(entry)).thenReturn(entry);

        var response = financialEntryService.markAsPaid(1L, LocalDate.of(2026, 5, 10));

        assertThat(response.status()).isEqualTo(EntryStatus.PAID);
        assertThat(response.paidAt()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    void monthSummaryDeveConsolidarEntradasESaidas() {
        List<FinancialEntry> entries = List.of(
                FinancialEntry.builder().direction(EntryDirection.RECEIVABLE).status(EntryStatus.PENDING).amount(new BigDecimal("100.00")).dueDate(LocalDate.of(2026, 5, 10)).category("A").build(),
                FinancialEntry.builder().direction(EntryDirection.PAYABLE).status(EntryStatus.PENDING).amount(new BigDecimal("30.00")).dueDate(LocalDate.of(2026, 5, 11)).category("B").build(),
                FinancialEntry.builder().direction(EntryDirection.RECEIVABLE).status(EntryStatus.PAID).amount(new BigDecimal("50.00")).dueDate(LocalDate.of(2026, 5, 12)).category("C").build()
        );
        when(financialEntryRepository.search(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                null,
                null,
                null
        )).thenReturn(entries);

        FinancialSummaryResponse summary = financialEntryService.monthSummary(YearMonth.of(2026, 5));

        assertThat(summary.income()).isEqualByComparingTo("150.00");
        assertThat(summary.expense()).isEqualByComparingTo("30.00");
        assertThat(summary.balance()).isEqualByComparingTo("120.00");
        assertThat(summary.pendingReceivable()).isEqualByComparingTo("100.00");
        assertThat(summary.pendingPayable()).isEqualByComparingTo("30.00");
    }
}
