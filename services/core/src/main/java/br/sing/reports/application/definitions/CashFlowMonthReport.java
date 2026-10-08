package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Component
public class CashFlowMonthReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                due_date,
                direction,
                status,
                category,
                amount,
                description
            FROM financial_entry
            WHERE is_active = TRUE
              AND due_date BETWEEN :startDate AND :endDate
            ORDER BY due_date, id
            """;

    public CashFlowMonthReport(ReportJdbcRepository jdbc) {
        super(jdbc);
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
        Map<String, Object> params = params(filterValues);
        params.put("startDate", ym.atDay(1));
        params.put("endDate", ym.atEndOfMonth());
        return jdbc.queryAutoColumns(SQL, params);
    }
}
