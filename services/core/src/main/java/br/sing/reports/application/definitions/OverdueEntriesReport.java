package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportColumnType;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.remote.FinanceClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OverdueEntriesReport extends RemoteReport implements ReportDefinition {

    private static final Set<String> OPEN_STATUSES = Set.of("PENDING", "OVERDUE");

    private static final List<ReportColumn> COLUMNS = List.of(
            column("id", ReportColumnType.NUMBER),
            column("due_date", ReportColumnType.DATE),
            column("direction", ReportColumnType.STRING),
            column("category", ReportColumnType.STRING),
            column("amount", ReportColumnType.NUMBER),
            column("description", ReportColumnType.STRING),
            column("sponsor_name", ReportColumnType.STRING)
    );

    private final FinanceClient financeClient;

    public OverdueEntriesReport(FinanceClient financeClient) {
        this.financeClient = financeClient;
    }

    @Override
    public String slug() {
        return "contas-vencidas";
    }

    @Override
    public String displayName() {
        return "Contas vencidas";
    }

    @Override
    public String description() {
        return "Contas pendentes com vencimento anterior à data atual.";
    }

    @Override
    public String category() {
        return "Financeiro";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of();
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        LocalDate today = LocalDate.now();

        List<FinanceClient.Entry> entries = financeClient.listEntries(null).stream()
                .filter(FinanceClient.Entry::ativo)
                .filter(e -> OPEN_STATUSES.contains(e.status()))
                .filter(e -> e.dueDate() != null && e.dueDate().isBefore(today))
                .sorted(Comparator.comparing(FinanceClient.Entry::dueDate)
                        .thenComparing(FinanceClient.Entry::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<List<Object>> rows = new ArrayList<>(entries.size());
        for (FinanceClient.Entry e : entries) {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(e.id());
            row.add(e.dueDate());
            row.add(e.direction());
            row.add(e.category());
            row.add(e.amount());
            row.add(e.description());
            row.add(e.sponsorName());
            rows.add(row);
        }
        return result(COLUMNS, rows);
    }
}
