package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportColumnType;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.remote.FinanceClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class EntriesByCategoryReport extends RemoteReport implements ReportDefinition {

    private static final List<ReportColumn> COLUMNS = List.of(
            column("category", ReportColumnType.STRING),
            column("total_receivable", ReportColumnType.NUMBER),
            column("total_payable", ReportColumnType.NUMBER),
            column("balance", ReportColumnType.NUMBER)
    );

    private final FinanceClient financeClient;

    public EntriesByCategoryReport(FinanceClient financeClient) {
        this.financeClient = financeClient;
    }

    @Override
    public String slug() {
        return "lancamentos-por-categoria";
    }

    @Override
    public String displayName() {
        return "Lançamentos por categoria";
    }

    @Override
    public String description() {
        return "Consolidação de entradas e saídas por categoria no período.";
    }

    @Override
    public String category() {
        return "Financeiro";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return periodFilters();
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        LocalDate start = (LocalDate) filterValues.get("start");
        LocalDate end = (LocalDate) filterValues.get("end");

        Map<String, BigDecimal[]> totals = new TreeMap<>();
        financeClient.listEntries(null).stream()
                .filter(FinanceClient.Entry::ativo)
                .filter(e -> e.dueDate() != null && !e.dueDate().isBefore(start) && !e.dueDate().isAfter(end))
                .forEach(e -> {
                    BigDecimal amount = e.amount() == null ? BigDecimal.ZERO : e.amount();
                    BigDecimal[] sums = totals.computeIfAbsent(
                            e.category(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                    boolean receivable = "RECEIVABLE".equals(e.direction());
                    if (receivable) {
                        sums[0] = sums[0].add(amount);
                    } else if ("PAYABLE".equals(e.direction())) {
                        sums[1] = sums[1].add(amount);
                    }
                    sums[2] = receivable ? sums[2].add(amount) : sums[2].subtract(amount);
                });

        List<List<Object>> rows = new ArrayList<>(totals.size());
        totals.forEach((category, sums) -> {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(category);
            row.add(sums[0]);
            row.add(sums[1]);
            row.add(sums[2]);
            rows.add(row);
        });
        return result(COLUMNS, rows);
    }
}
