package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AttendanceByRoomReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            WITH RECURSIVE dates(dt) AS (
                SELECT CAST(:start AS DATE)
                UNION ALL
                SELECT dt + INTERVAL '1 day' FROM dates WHERE dt < :end
            ),
            expected AS (
                SELECT sr.id AS schoolroom_id, COUNT(*) AS expected_lectures
                FROM schoolroom sr
                INNER JOIN schoolroom_schedule_slot slot ON slot.schoolroom_id = sr.id AND slot.is_active = TRUE
                CROSS JOIN dates d
                WHERE sr.is_active = TRUE
                  AND (:workshopId IS NULL OR sr.workshop_id = :workshopId)
                  AND (
                    (EXTRACT(ISODOW FROM d.dt) = 1 AND slot.day_of_week = 'MONDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 2 AND slot.day_of_week = 'TUESDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 3 AND slot.day_of_week = 'WEDNESDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 4 AND slot.day_of_week = 'THURSDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 5 AND slot.day_of_week = 'FRIDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 6 AND slot.day_of_week = 'SATURDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 7 AND slot.day_of_week = 'SUNDAY')
                  )
                GROUP BY sr.id
            )
            SELECT
                sr.id AS schoolroom_id,
                sr.name AS schoolroom_name,
                w.name AS workshop_name,
                tp.name AS teacher_name,
                COALESCE(e.expected_lectures, 0) AS expected_lectures,
                COUNT(DISTINCT l.id) AS registered_lectures,
                COALESCE(e.expected_lectures, 0) - COUNT(DISTINCT l.id) AS pending_lectures,
                SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS presences,
                SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absences,
                SUM(CASE WHEN a.status = 'JUSTIFIED' THEN 1 ELSE 0 END) AS justified,
                CASE WHEN COUNT(a.id) = 0 THEN 0
                     ELSE ROUND(100.0 * SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(a.id), 1)
                END AS attendance_pct,
                (SELECT COUNT(DISTINCT en.student_id) FROM enrollment en
                 WHERE en.schoolroom_id = sr.id AND en.status = TRUE AND en.is_active = TRUE) AS active_students
            FROM schoolroom sr
            INNER JOIN workshop w ON w.id = sr.workshop_id
            LEFT JOIN person tp ON tp.id = sr.teacher_id
            LEFT JOIN expected e ON e.schoolroom_id = sr.id
            LEFT JOIN lecture l ON l.schoolroom_id = sr.id
                AND CAST(l.date_time AS DATE) BETWEEN :start AND :end AND l.is_active = TRUE
            LEFT JOIN attendance a ON a.lecture_id = l.id AND a.is_active = TRUE
            WHERE sr.is_active = TRUE
              AND (:workshopId IS NULL OR sr.workshop_id = :workshopId)
            GROUP BY sr.id, sr.name, w.name, tp.name, e.expected_lectures
            ORDER BY sr.name
            """;

    public AttendanceByRoomReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "frequencia-por-turma";
    }

    @Override
    public String displayName() {
        return "Frequência consolidada por turma";
    }

    @Override
    public String description() {
        return "Resumo de aulas previstas, registradas, presenças e faltas por turma no período.";
    }

    @Override
    public String category() {
        return "Frequência";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(
                ReportFilterSpec.date("start", "Data inicial", true),
                ReportFilterSpec.date("end", "Data final", true),
                ReportFilterSpec.lookup("workshopId", "Oficina", "workshops")
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        Map<String, Object> params = params(filterValues);
        return jdbc.queryAutoColumns(SQL, params);
    }
}
