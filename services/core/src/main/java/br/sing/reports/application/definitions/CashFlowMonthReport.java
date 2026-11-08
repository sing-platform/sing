package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportColumnType;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.remote.FinanceClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class CashFlowMonthReport extends RemoteReport implements ReportDefinition {

    private static final List<ReportColumn> COLUMNS = List.of(
            column("due_date", ReportColumnType.DATE),
            column("direction", ReportColumnType.STRING),
            column("status", ReportColumnType.STRING),
            column("category", ReportColumnType.STRING),
            column("amount", ReportColumnType.NUMBER),
            column("description", ReportColumnType.STRING)
    );

    private final FinanceClient financeClient;

    public CashFlowMonthReport(FinanceClient financeClient) {
        this.financeClient = financeClient;
    }

    @Override
    public String slug() {
        return "fluxo-caixa-mes";
    }

    @Override
    public String displayName() {
        return "Fluxo de caixa do mês";
    }

    @Override
    public String description() {
        return "Entradas e saídas financeiras no mês selecionado.";
    }

    @Override
    public String category() {
        return "Financeiro";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(ReportFilterSpec.optionalText("month", "Mês (YYYY-MM)"));
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        Object monthValue = filterValues.get("month");
        YearMonth ym = (monthValue == null || String.valueOf(monthValue).isBlank())
                ? YearMonth.now()
                : YearMonth.parse(String.valueOf(monthValue));
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<FinanceClient.Entry> entries = financeClient.listEntries(ym.toString()).stream()
                .filter(FinanceClient.Entry::ativo)
                .filter(e -> e.dueDate() != null && !e.dueDate().isBefore(start) && !e.dueDate().isAfter(end))
                .sorted(Comparator.comparing(FinanceClient.Entry::dueDate)
                        .thenComparing(FinanceClient.Entry::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<List<Object>> rows = new ArrayList<>(entries.size());
        for (FinanceClient.Entry e : entries) {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(e.dueDate());
            row.add(e.direction());
            row.add(e.status());
            row.add(e.category());
            row.add(e.amount());
            row.add(e.description());
            rows.add(row);
        }
        return result(COLUMNS, rows);
    }
}
