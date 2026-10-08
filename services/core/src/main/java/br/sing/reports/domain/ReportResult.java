package br.sing.reports.domain;

import java.util.List;
import java.util.Map;

public record ReportResult(
        List<ReportColumn> columns,
        List<List<Object>> rows,
        Map<String, Object> totals,
        long totalRows
) {
    public static ReportResult empty(List<ReportColumn> columns) {
        return new ReportResult(columns, List.of(), Map.of(), 0);
    }
}
