package br.cadi.finance.application;

import br.cadi.finance.domain.Sponsor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SponsorBillingJob {

    private final SponsorService sponsorService;
    private final FinancialEntryService financialEntryService;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void generateMonthlyReceivables() {
        LocalDate now = LocalDate.now();
        LocalDate endOfMonth = YearMonth.from(now).atEndOfMonth();
        List<Sponsor> sponsors = sponsorService.findActiveForBilling();

        for (Sponsor sponsor : sponsors) {
            LocalDate baseDate = sponsor.getLastGeneratedDate() != null
                    ? sponsor.getLastGeneratedDate()
                    : sponsor.getStartDate().minusMonths(1);

            LocalDate nextDueDate = sponsor.getFrequency().nextDueDate(baseDate);
            if (nextDueDate.isAfter(endOfMonth)) {
                continue;
            }
            if (financialEntryService.existsSponsorDueDate(sponsor.getId(), nextDueDate)) {
                continue;
            }

            financialEntryService.createForSponsorBilling(sponsor, nextDueDate);
            sponsor.setLastGeneratedDate(nextDueDate);
        }

        sponsorService.saveAll(sponsors);
        log.info("Financeiro: geração automática de cobranças de padrinhos concluída.");
    }
}
