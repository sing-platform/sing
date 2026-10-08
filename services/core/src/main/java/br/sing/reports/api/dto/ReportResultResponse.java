package br.sing.reports.api.dto;

import java.util.List;
import java.util.Map;

public record ReportResultResponse(
        String slug,
        String displayName,
        List<String> columns,
        List<String> columnKeys,
        List<List<Object>> rows,
        Map<String, Object> totals,
        long totalRows
) {
}
