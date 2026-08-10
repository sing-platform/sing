package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OverdueEntriesReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                e.id,
                e.due_date,
                e.direction,
                e.category,
                e.amount,
                e.description,
                s.name AS sponsor_name
            FROM financial_entry e
            LEFT JOIN sponsor s ON s.id = e.sponsor_id
            WHERE e.is_active = TRUE
              AND e.status = 'PENDING'
              AND e.due_date < :today
            ORDER BY e.due_date ASC
            """;

    public OverdueEntriesReport(ReportJdbcRepository jdbc) {
        super(jdbc);
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
        Map<String, Object> p = params(filterValues);
        p.put("today", java.time.LocalDate.now());
        return jdbc.queryAutoColumns(SQL, p);
    }
}
