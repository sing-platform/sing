package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StudentPreRegisterListReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                spr.id,
                spr.name,
                spr.email,
                spr.phone,
                w.name AS workshop_name,
                spr.shift,
                spr.created_at
            FROM student_pre_register spr
            INNER JOIN workshop w ON w.id = spr.workshop_id
            WHERE (CAST(:workshopId AS BIGINT) IS NULL OR spr.workshop_id = :workshopId)
              AND (CAST(:shift AS VARCHAR) IS NULL OR spr.shift = :shift)
              AND (CAST(:start AS DATE) IS NULL OR CAST(spr.created_at AS DATE) >= :start)
              AND (CAST(:end AS DATE)   IS NULL OR CAST(spr.created_at AS DATE) <= :end)
            ORDER BY spr.created_at ASC
            """;

    public StudentPreRegisterListReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "pre-cadastros-lista";
    }

    @Override
    public String displayName() {
        return "Lista de espera (pré-cadastros)";
    }

    @Override
    public String description() {
        return "Candidatos cadastrados na fila de espera, ordenados por data (mais antigos primeiro).";
    }

    @Override
    public String category() {
        return "Acadêmico";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(
                ReportFilterSpec.lookup("workshopId", "Oficina", "workshops"),
                ReportFilterSpec.selectOf("shift", "Turno",
                        java.util.List.of("MORNING", "AFTERNOON", "EVENING")),
                ReportFilterSpec.date("start", "Cadastrado a partir de", false),
                ReportFilterSpec.date("end", "Cadastrado até", false)
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
