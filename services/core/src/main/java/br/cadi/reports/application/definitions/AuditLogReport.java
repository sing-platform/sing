package br.cadi.reports.application.definitions;

import br.cadi.reports.application.ReportDefinition;
import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AuditLogReport extends AbstractReport implements ReportDefinition {

    private static final String SQL = """
            SELECT
                al.id,
                al.table_name,
                al.record_id,
                al.action,
                al.changed_by,
                al.changed_at
            FROM audit_log al
            WHERE CAST(al.changed_at AS DATE) BETWEEN :start AND :end
              AND (CAST(:tableName AS VARCHAR) IS NULL OR al.table_name = :tableName)
              AND (CAST(:changedBy AS BIGINT) IS NULL OR al.changed_by = :changedBy)
            ORDER BY al.changed_at DESC
            """;

    public AuditLogReport(ReportJdbcRepository jdbc) {
        super(jdbc);
    }

    @Override
    public String slug() {
        return "auditoria";
    }

    @Override
    public String displayName() {
        return "Log de auditoria";
    }

    @Override
    public String description() {
        return "Alterações registradas por tabela, usuário e período.";
    }

    @Override
    public String category() {
        return "Operacional";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of(
                ReportFilterSpec.date("start", "Data inicial", true),
                ReportFilterSpec.date("end", "Data final", true),
                ReportFilterSpec.selectOf("tableName", "Tabela", java.util.List.of(
                        "person", "student", "guardian", "collaborator",
                        "schoolroom", "lecture", "attendance", "enrollment",
                        "descriptive_report", "user_documents", "mural_post"
                )),
                ReportFilterSpec.optionalNumber("changedBy", "ID do usuário")
        );
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        return jdbc.queryAutoColumns(SQL, params(filterValues));
    }
}
