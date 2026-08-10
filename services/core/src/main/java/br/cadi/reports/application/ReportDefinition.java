package br.cadi.reports.application;

import br.cadi.reports.domain.ReportFilterSpec;
import br.cadi.reports.domain.ReportResult;

import java.util.List;
import java.util.Map;

public interface ReportDefinition {

    String slug();

    String displayName();

    String description();

    List<ReportFilterSpec> filters();

    ReportResult execute(Map<String, Object> filterValues);

    default String category() {
        return "Geral";
    }
}
