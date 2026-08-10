package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StudentAttendanceDetailReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                sp.name AS student_name,
                sr.name AS schoolroom_name,
                CAST(l.date_time AS DATE) AS class_date,
                l.topic,
                a.status,
                a.observation
            FROM attendance a
            INNER JOIN lecture l ON l.id = a.lecture_id AND l.is_active = TRUE
            INNER JOIN schoolroom sr ON sr.id = l.schoolroom_id
            INNER JOIN person sp ON sp.id = a.student_id
            WHERE a.is_active = TRUE
              AND CAST(l.date_time AS DATE) BETWEEN :start AND :end
              AND (:studentId IS NULL OR a.student_id = :studentId)
              AND (:schoolroomId IS NULL OR l.schoolroom_id = :schoolroomId)
            ORDER BY sp.name, l.date_time
            """;

    public StudentAttendanceDetailReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "presenca-detalhada-aluno";
    }

    @Override
    public String displayName() {
        return "Histórico de presença por aluno";
    }

    @Override
    public String description() {
        return "Lista detalhada de presenças, faltas e justificativas por aula.";
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
                ReportFilterSpec.lookup("studentId", "Aluno", "students"),
                ReportFilterSpec.lookup("schoolroomId", "Turma", "schoolrooms")
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
