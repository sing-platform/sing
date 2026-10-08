package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BirthdaysOfMonthReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT person_type, name, birth_date, email, phone
            FROM (
                SELECT 'ALUNO' AS person_type, p.name, p.birth_date, p.email, p.phone
                FROM student s INNER JOIN person p ON p.id = s.person_id AND p.is_active = TRUE
                UNION ALL
                SELECT 'COLABORADOR', p.name, p.birth_date, p.email, p.phone
                FROM collaborator c INNER JOIN person p ON p.id = c.person_id AND p.is_active = TRUE
                UNION ALL
                SELECT 'RESPONSAVEL', p.name, p.birth_date, p.email, p.phone
                FROM guardian g INNER JOIN person p ON p.id = g.person_id AND p.is_active = TRUE
            ) all_people
            WHERE EXTRACT(MONTH FROM birth_date) = :month
            ORDER BY EXTRACT(DAY FROM birth_date), name
            """;

    public BirthdaysOfMonthReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "aniversariantes-mes";
    }

    @Override
    public String displayName() {
        return "Aniversariantes do mês";
    }

    @Override
    public String description() {
        return "Alunos, colaboradores e responsáveis que fazem aniversário no mês.";
    }

    @Override
    public String category() {
        return "Pessoas";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(
                ReportFilterSpec.number("month", "Mês (1-12)", true,
                        java.time.LocalDate.now().getMonthValue())
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
