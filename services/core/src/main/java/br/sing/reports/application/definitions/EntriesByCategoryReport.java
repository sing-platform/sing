package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EntriesByCategoryReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                category,
                SUM(CASE WHEN direction = 'RECEIVABLE' THEN amount ELSE 0 END) AS total_receivable,
                SUM(CASE WHEN direction = 'PAYABLE' THEN amount ELSE 0 END) AS total_payable,
                SUM(CASE WHEN direction = 'RECEIVABLE' THEN amount ELSE -amount END) AS balance
            FROM financial_entry
            WHERE is_active = TRUE
              AND due_date BETWEEN :start AND :end
            GROUP BY category
            ORDER BY category
            """;

    public EntriesByCategoryReport(ReportJdbcRepository jdbc) {
        super(jdbc);
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
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
