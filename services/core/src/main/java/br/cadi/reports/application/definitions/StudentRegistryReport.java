package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StudentRegistryReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                p.name AS student_name,
                p.cpf,
                p.email,
                p.phone,
                s.grade,
                s.current_school,
                sr.name AS schoolroom_name,
                a.city,
                a.state,
                s.allergy,
                s.chronic_illness,
                (SELECT pg.name FROM student_guardian sg
                 INNER JOIN guardian g ON g.person_id = sg.guardian_id
                 INNER JOIN person pg ON pg.id = g.person_id
                 WHERE sg.student_id = s.person_id AND g.is_primary = TRUE
                 ORDER BY sg.id FETCH FIRST 1 ROW ONLY) AS primary_guardian
            FROM student s
            INNER JOIN person p ON p.id = s.person_id AND p.is_active = TRUE
            LEFT JOIN address a ON a.id = p.address_id
            LEFT JOIN enrollment en ON en.student_id = s.person_id AND en.status = TRUE AND en.is_active = TRUE
            LEFT JOIN schoolroom sr ON sr.id = en.schoolroom_id
            WHERE (:schoolroomId IS NULL OR en.schoolroom_id = :schoolroomId)
            ORDER BY p.name
            """;

    public StudentRegistryReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "cadastro-alunos";
    }

    @Override
    public String displayName() {
        return "Cadastro consolidado de alunos";
    }

    @Override
    public String description() {
        return "Dados cadastrais, turma, endereço e informações de saúde.";
    }

    @Override
    public String category() {
        return "Acadêmico";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(ReportFilterSpec.lookup("schoolroomId", "Turma", "schoolrooms"));
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
