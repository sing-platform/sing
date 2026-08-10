package br.cadi.finance.application;

import br.cadi.finance.domain.Sponsor;
import br.cadi.finance.domain.SponsorFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SponsorBillingJobTest {

    @Mock
    private SponsorService sponsorService;
    @Mock
    private FinancialEntryService financialEntryService;
    @InjectMocks
    private SponsorBillingJob sponsorBillingJob;

    @Test
    void deveGerarCobrancaQuandoPadrinhoAtivoTemParcelaNoMes() {
        Sponsor sponsor = Sponsor.builder()
                .id(1L)
                .name("Padrinho A")
                .amount(new BigDecimal("100.00"))
                .frequency(SponsorFrequency.MONTHLY)
                .startDate(LocalDate.now().minusMonths(2))
                .lastGeneratedDate(LocalDate.now().minusMonths(1).withDayOfMonth(1))
                .build();
        when(sponsorService.findActiveForBilling()).thenReturn(List.of(sponsor));
        when(financialEntryService.existsSponsorDueDate(eq(1L), any(LocalDate.class))).thenReturn(false);

        sponsorBillingJob.generateMonthlyReceivables();

        verify(financialEntryService, times(1)).createForSponsorBilling(eq(sponsor), any(LocalDate.class));
        verify(sponsorService).saveAll(List.of(sponsor));
    }

    @Test
    void naoDeveDuplicarQuandoParcelaJaExiste() {
        Sponsor sponsor = Sponsor.builder()
                .id(2L)
                .name("Padrinho B")
                .amount(new BigDecimal("90.00"))
                .frequency(SponsorFrequency.MONTHLY)
                .startDate(LocalDate.now().minusMonths(2))
                .lastGeneratedDate(LocalDate.now().minusMonths(1).withDayOfMonth(1))
                .build();
        when(sponsorService.findActiveForBilling()).thenReturn(List.of(sponsor));
        when(financialEntryService.existsSponsorDueDate(eq(2L), any(LocalDate.class))).thenReturn(true);

        sponsorBillingJob.generateMonthlyReceivables();

        verify(financialEntryService, never()).createForSponsorBilling(any(), any());
    }
}
