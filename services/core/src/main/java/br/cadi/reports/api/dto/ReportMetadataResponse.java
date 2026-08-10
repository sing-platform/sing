package br.cadi.reports.api.dto;

import java.util.List;

public record ReportMetadataResponse(
        String slug,
        String displayName,
        String description,
        String category,
        List<ReportFilterResponse> filters
) {
}
