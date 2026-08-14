package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TeacherWorkloadReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                p.name AS teacher_name,
                sr.name AS schoolroom_name,
                w.name AS workshop_name,
                slot.day_of_week,
                slot.start_time,
                slot.end_time
            FROM schoolroom sr
            INNER JOIN schoolroom_schedule_slot slot ON slot.schoolroom_id = sr.id AND slot.is_active = TRUE
            INNER JOIN person p ON p.id = sr.teacher_id
            INNER JOIN workshop w ON w.id = sr.workshop_id
            WHERE sr.is_active = TRUE AND sr.teacher_id IS NOT NULL
              AND (CAST(:teacherId AS BIGINT) IS NULL OR sr.teacher_id = :teacherId)
            ORDER BY p.name, slot.day_of_week, slot.start_time
            """;

    public TeacherWorkloadReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "carga-horaria-professor";
    }

    @Override
    public String displayName() {
        return "Carga horária do professor";
    }

    @Override
    public String description() {
        return "Turmas, dias e horários de cada professor titular.";
    }

    @Override
    public String category() {
        return "Pessoas";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(ReportFilterSpec.lookup("teacherId", "Professor", "teachers"));
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
