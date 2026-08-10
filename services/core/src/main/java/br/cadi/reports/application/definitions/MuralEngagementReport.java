package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MuralEngagementReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                mp.id AS post_id,
                mp.title,
                mp.created_at,
                u.username AS author_username,
                (SELECT COUNT(*) FROM mural_post_view v WHERE v.post_id = mp.id) AS view_count,
                (SELECT COUNT(*) FROM mural_post_view v WHERE v.post_id = mp.id AND v.viewed_at IS NOT NULL) AS read_count
            FROM mural_post mp
            INNER JOIN users u ON u.id = mp.author_id
            WHERE mp.is_active = TRUE
              AND CAST(mp.created_at AS DATE) BETWEEN :start AND :end
            ORDER BY mp.created_at DESC
            """;

    public MuralEngagementReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "engajamento-mural";
    }

    @Override
    public String displayName() {
        return "Engajamento do mural";
    }

    @Override
    public String description() {
        return "Posts publicados e visualizações no período.";
    }

    @Override
    public String category() {
        return "Operacional";
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
