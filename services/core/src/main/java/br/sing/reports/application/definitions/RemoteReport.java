package br.sing.reports.application.definitions;

import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportColumnType;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;

import java.util.List;
import java.util.Map;

abstract class RemoteReport {

    protected static List<ReportFilterSpec> periodFilters() {
        return AbstractReport.periodFilters();
    }

    protected static ReportColumn column(String key, ReportColumnType type) {
        return new ReportColumn(key, key, type);
    }

    protected static ReportResult result(List<ReportColumn> columns, List<List<Object>> rows) {
        List<List<Object>> limited = rows.size() > ReportJdbcRepository.MAX_ROWS
                ? rows.subList(0, ReportJdbcRepository.MAX_ROWS)
                : rows;
        return new ReportResult(columns, limited, Map.of(), limited.size());
    }
}
