package br.sing.reports.domain;

public record ReportColumn(
        String key,
        String label,
        ReportColumnType type
) {
}
