package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StudentsAtRiskReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                s.person_id AS student_id,
                sp.name AS student_name,
                sr.id AS schoolroom_id,
                sr.name AS schoolroom_name,
                COUNT(a.id) AS total_records,
                SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS presences,
                SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absences,
                SUM(CASE WHEN a.status = 'JUSTIFIED' THEN 1 ELSE 0 END) AS justified,
                CASE WHEN COUNT(a.id) = 0 THEN 0
                     ELSE ROUND(100.0 * SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(a.id), 1)
                END AS attendance_pct,
                (SELECT pg.name FROM student_guardian sg
                 INNER JOIN guardian g ON g.person_id = sg.guardian_id
                 INNER JOIN person pg ON pg.id = g.person_id
                 WHERE sg.student_id = s.person_id AND sg.is_active = TRUE AND g.is_primary = TRUE
                 ORDER BY sg.id LIMIT 1) AS primary_guardian_name,
                (SELECT pg.phone FROM student_guardian sg
                 INNER JOIN guardian g ON g.person_id = sg.guardian_id
                 INNER JOIN person pg ON pg.id = g.person_id
                 WHERE sg.student_id = s.person_id AND sg.is_active = TRUE AND g.is_primary = TRUE
                 ORDER BY sg.id LIMIT 1) AS primary_guardian_phone,
                (SELECT pg.email FROM student_guardian sg
                 INNER JOIN guardian g ON g.person_id = sg.guardian_id
                 INNER JOIN person pg ON pg.id = g.person_id
                 WHERE sg.student_id = s.person_id AND sg.is_active = TRUE AND g.is_primary = TRUE
                 ORDER BY sg.id LIMIT 1) AS primary_guardian_email
            FROM student s
            INNER JOIN person sp ON sp.id = s.person_id AND sp.is_active = TRUE
            INNER JOIN enrollment en ON en.student_id = s.person_id AND en.status = TRUE AND en.is_active = TRUE
            INNER JOIN schoolroom sr ON sr.id = en.schoolroom_id AND sr.is_active = TRUE
            INNER JOIN lecture l ON l.schoolroom_id = sr.id
                AND CAST(l.date_time AS DATE) BETWEEN :start AND :end AND l.is_active = TRUE
            INNER JOIN attendance a ON a.lecture_id = l.id AND a.student_id = s.person_id AND a.is_active = TRUE
            WHERE (CAST(:schoolroomId AS BIGINT) IS NULL OR sr.id = :schoolroomId)
            GROUP BY s.person_id, sp.name, sr.id, sr.name
            HAVING COUNT(a.id) > 0
               AND (100.0 * SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(a.id)) < :thresholdPct
            ORDER BY attendance_pct ASC, sp.name
            """;

    public StudentsAtRiskReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "alunos-em-risco";
    }

    @Override
    public String displayName() {
        return "Alunos em risco (frequência baixa)";
    }

    @Override
    public String description() {
        return "Alunos com percentual de presença abaixo do limiar no período.";
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
                ReportFilterSpec.number("thresholdPct", "Limiar de presença (%)", false, 75.0),
                ReportFilterSpec.lookup("schoolroomId", "Turma", "schoolrooms")
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        Object threshold = filterValues.getOrDefault("thresholdPct", 75.0);
        Map<String, Object> params = params(filterValues);
        params.putIfAbsent("thresholdPct", threshold instanceof Number n ? n.doubleValue() : 75.0);
        return jdbc.queryAutoColumns(SQL, params);
    }
}
