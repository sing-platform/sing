package br.sing.reports.application.definitions;

import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;

import java.util.List;
import java.util.Map;

abstract class AbstractReport {

    protected final ReportJdbcRepository jdbc;

    protected AbstractReport(ReportJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    protected static List<ReportFilterSpec> periodFilters() {
        return List.of(
                ReportFilterSpec.date("start", "Data inicial", true),
                ReportFilterSpec.date("end", "Data final", true)
        );
    }

    protected Map<String, Object> params(Map<String, Object> filters) {
        return new java.util.HashMap<>(filters);
    }
}
