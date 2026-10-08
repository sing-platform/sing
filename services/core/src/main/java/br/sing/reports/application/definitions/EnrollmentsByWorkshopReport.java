package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EnrollmentsByWorkshopReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                w.name AS workshop_name,
                sr.id AS schoolroom_id,
                sr.name AS schoolroom_name,
                sr.capacity,
                COUNT(DISTINCT en.student_id) AS enrolled_students,
                CASE WHEN sr.capacity IS NULL OR sr.capacity = 0 THEN NULL
                     ELSE ROUND(100.0 * COUNT(DISTINCT en.student_id) / sr.capacity, 1)
                END AS occupancy_pct,
                CASE WHEN sr.capacity IS NULL THEN NULL
                     WHEN sr.capacity - COUNT(DISTINCT en.student_id) < 0 THEN 0
                     ELSE sr.capacity - COUNT(DISTINCT en.student_id)
                END AS free_slots
            FROM schoolroom sr
            INNER JOIN workshop w ON w.id = sr.workshop_id
            LEFT JOIN enrollment en ON en.schoolroom_id = sr.id AND en.status = TRUE AND en.is_active = TRUE
            WHERE sr.is_active = TRUE
              AND (CAST(:workshopId AS BIGINT) IS NULL OR w.id = :workshopId)
            GROUP BY w.name, sr.id, sr.name, sr.capacity
            ORDER BY w.name, sr.name
            """;

    public EnrollmentsByWorkshopReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "matriculas-por-turma";
    }

    @Override
    public String displayName() {
        return "Matrículas ativas por oficina/turma";
    }

    @Override
    public String description() {
        return "Ocupação e vagas disponíveis por turma.";
    }

    @Override
    public String category() {
        return "Acadêmico";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(ReportFilterSpec.lookup("workshopId", "Oficina", "workshops"));
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
