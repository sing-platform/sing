package br.sing.reports.api.dto;

import java.util.List;

public record ReportFilterResponse(
        String name,
        String label,
        String type,
        boolean required,
        Object defaultValue,
        List<String> options,
        String optionsSource
) {
}
