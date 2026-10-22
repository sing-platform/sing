package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PendingCallsReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            WITH RECURSIVE dates(dt) AS (
                SELECT CAST(:start AS DATE)
                UNION ALL
                -- date+int no PostgreSQL ja e date. O CAST segura o tipo no UNION
                -- recursivo: sem ele o H2 infere outro tipo e o teste quebra.
                SELECT CAST(dt + 1 AS DATE) FROM dates WHERE dt < :end
            ),
            expected_days AS (
                SELECT sr.id AS schoolroom_id, sr.name AS schoolroom_name,
                       sr.teacher_id, tp.name AS teacher_name, d.dt AS class_date
                FROM schoolroom sr
                INNER JOIN schoolroom_schedule_slot slot ON slot.schoolroom_id = sr.id AND slot.is_active = TRUE
                CROSS JOIN dates d
                LEFT JOIN person tp ON tp.id = sr.teacher_id
                WHERE sr.is_active = TRUE AND sr.teacher_id IS NOT NULL
                  AND (CAST(:teacherId AS BIGINT) IS NULL OR sr.teacher_id = :teacherId)
                  AND (
                    (EXTRACT(ISODOW FROM d.dt) = 1 AND slot.day_of_week = 'MONDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 2 AND slot.day_of_week = 'TUESDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 3 AND slot.day_of_week = 'WEDNESDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 4 AND slot.day_of_week = 'THURSDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 5 AND slot.day_of_week = 'FRIDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 6 AND slot.day_of_week = 'SATURDAY')
                    OR (EXTRACT(ISODOW FROM d.dt) = 7 AND slot.day_of_week = 'SUNDAY')
                  )
            ),
            registered AS (
                SELECT l.schoolroom_id, CAST(l.date_time AS DATE) AS class_date,
                       CASE WHEN EXISTS (SELECT 1 FROM attendance a WHERE a.lecture_id = l.id AND a.is_active = TRUE)
                            THEN 1 ELSE 0 END AS has_attendance
                FROM lecture l
                WHERE l.is_active = TRUE AND CAST(l.date_time AS DATE) BETWEEN :start AND :end
            )
            SELECT ed.teacher_id, ed.teacher_name, ed.schoolroom_id, ed.schoolroom_name,
                   ed.class_date, COALESCE(r.has_attendance, 0) AS has_attendance,
                   CASE WHEN r.schoolroom_id IS NULL OR COALESCE(r.has_attendance, 0) = 0 THEN 1 ELSE 0 END AS is_pending
            FROM expected_days ed
            LEFT JOIN registered r ON r.schoolroom_id = ed.schoolroom_id AND r.class_date = ed.class_date
            WHERE r.schoolroom_id IS NULL OR COALESCE(r.has_attendance, 0) = 0
            ORDER BY ed.teacher_name, ed.schoolroom_name, ed.class_date
            """;

    public PendingCallsReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "chamadas-pendentes";
    }

    @Override
    public String displayName() {
        return "Chamadas pendentes por professor";
    }

    @Override
    public String description() {
        return "Aulas previstas pela grade sem chamada registrada no período.";
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
                ReportFilterSpec.lookup("teacherId", "Professor", "teachers")
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        Map<String, Object> params = params(filterValues);
        return jdbc.queryAutoColumns(SQL, params);
    }
}
