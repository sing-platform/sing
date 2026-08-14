package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DocumentsByUserReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                ud.id,
                p.name AS owner_name,
                ud.category,
                ud.scope,
                ud.original_file_name,
                ud.mime_type,
                ud.file_size,
                ud.uploaded_at,
                ud.active
            FROM user_documents ud
            INNER JOIN person p ON p.id = ud.owner_person_id
            WHERE (CAST(:ownerPersonId AS BIGINT) IS NULL OR ud.owner_person_id = :ownerPersonId)
              AND (CAST(:category AS VARCHAR) IS NULL OR ud.category = :category)
              AND (CAST(:activeOnly AS BOOLEAN) IS NULL OR ud.active = :activeOnly)
            ORDER BY ud.uploaded_at DESC
            """;

    public DocumentsByUserReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "documentos-usuario";
    }

    @Override
    public String displayName() {
        return "Documentos por usuário";
    }

    @Override
    public String description() {
        return "Lista de documentos enviados com categoria e status.";
    }

    @Override
    public String category() {
        return "Operacional";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(
                ReportFilterSpec.lookup("ownerPersonId", "Pessoa (aluno/colaborador)", "students"),
                ReportFilterSpec.selectOf("category", "Categoria", java.util.List.of(
                        "ATESTADO", "DECLARACAO", "RG", "CPF", "COMPROVANTE_RESIDENCIA",
                        "FOTO", "LAUDO_MEDICO", "OUTROS"
                )),
                ReportFilterSpec.selectOf("activeOnly", "Status", java.util.List.of("true", "false"))
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        Map<String, Object> p = new java.util.HashMap<>(filterValues);
        if (p.get("activeOnly") instanceof String s) {
            p.put("activeOnly", Boolean.parseBoolean(s));
        }
        return jdbc.queryAutoColumns(SQL, p);
    }
}
