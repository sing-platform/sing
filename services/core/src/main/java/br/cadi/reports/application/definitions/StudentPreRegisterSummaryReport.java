package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StudentPreRegisterSummaryReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                w.name AS workshop_name,
                spr.shift,
                COUNT(*) AS total_candidates,
                MIN(spr.created_at) AS oldest_registration,
                MAX(spr.created_at) AS newest_registration
            FROM student_pre_register spr
            INNER JOIN workshop w ON w.id = spr.workshop_id
            WHERE (:workshopId IS NULL OR spr.workshop_id = :workshopId)
            GROUP BY w.name, spr.shift
            ORDER BY w.name, spr.shift
            """;

    public StudentPreRegisterSummaryReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "pre-cadastros-resumo";
    }

    @Override
    public String displayName() {
        return "Demanda de pré-cadastros por oficina/turno";
    }

    @Override
    public String description() {
        return "Total de candidatos na lista de espera agrupados por oficina e turno, com a data do candidato mais antigo.";
    }

    @Override
    public String category() {
        return "Acadêmico";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(
                ReportFilterSpec.lookup("workshopId", "Oficina", "workshops")
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
