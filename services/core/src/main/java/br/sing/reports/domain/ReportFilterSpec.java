package br.sing.reports.domain;

import java.util.List;

public record ReportFilterSpec(
        String name,
        String label,
        ReportFilterType type,
        boolean required,
        Object defaultValue,
        List<String> options,
        String optionsSource
) {
    public static ReportFilterSpec date(String name, String label, boolean required) {
        return new ReportFilterSpec(name, label, ReportFilterType.DATE, required, null, null, null);
    }

    public static ReportFilterSpec number(String name, String label, boolean required, Object defaultValue) {
        return new ReportFilterSpec(name, label, ReportFilterType.NUMBER, required, defaultValue, null, null);
    }

    public static ReportFilterSpec optionalNumber(String name, String label) {
        return new ReportFilterSpec(name, label, ReportFilterType.NUMBER, false, null, null, null);
    }

    public static ReportFilterSpec optionalText(String name, String label) {
        return new ReportFilterSpec(name, label, ReportFilterType.TEXT, false, null, null, null);
    }

    public static ReportFilterSpec lookup(String name, String label, String optionsSource) {
        return new ReportFilterSpec(name, label, ReportFilterType.NUMBER, false, null, null, optionsSource);
    }

    public static ReportFilterSpec selectOf(String name, String label, List<String> options) {
        return new ReportFilterSpec(name, label, ReportFilterType.SELECT, false, null, options, null);
    }
}
