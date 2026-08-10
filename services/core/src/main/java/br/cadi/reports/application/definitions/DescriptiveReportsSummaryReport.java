package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DescriptiveReportsSummaryReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                sr.name AS schoolroom_name,
                ap.name AS author_name,
                dr.status,
                COUNT(*) AS total_reports,
                SUM(CASE WHEN dr.published_at IS NOT NULL THEN 1 ELSE 0 END) AS published_count
            FROM descriptive_report dr
            INNER JOIN schoolroom sr ON sr.id = dr.schoolroom_id
            INNER JOIN collaborator c ON c.person_id = dr.author_id
            INNER JOIN person ap ON ap.id = c.person_id
            WHERE dr.is_active = TRUE
              AND (:schoolroomId IS NULL OR dr.schoolroom_id = :schoolroomId)
              AND (:status IS NULL OR dr.status = :status)
            GROUP BY sr.name, ap.name, dr.status
            ORDER BY sr.name, dr.status
            """;

    public DescriptiveReportsSummaryReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "pareceres-por-status";
    }

    @Override
    public String displayName() {
        return "Pareceres descritivos por status";
    }

    @Override
    public String description() {
        return "Contagem de pareceres agrupados por turma, autor e status.";
    }

    @Override
    public String category() {
        return "Acadêmico";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(
                ReportFilterSpec.lookup("schoolroomId", "Turma", "schoolrooms"),
                ReportFilterSpec.selectOf("status", "Status",
                        java.util.List.of("DRAFT", "PUBLISHED", "CLOSED"))
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
