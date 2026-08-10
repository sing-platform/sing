package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ActiveSponsorsReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                s.id,
                s.name AS sponsor_name,
                p.name AS student_name,
                s.amount,
                s.frequency,
                s.start_date,
                s.last_generated_date
            FROM sponsor s
            INNER JOIN student st ON st.person_id = s.student_id
            INNER JOIN person p ON p.id = st.person_id
            WHERE s.is_active = TRUE
            ORDER BY s.name
            """;

    public ActiveSponsorsReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "padrinhos-ativos";
    }

    @Override
    public String displayName() {
        return "Padrinhos ativos";
    }

    @Override
    public String description() {
        return "Lista de padrinhos ativos com aluno vinculado e valor de contribuição.";
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
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
