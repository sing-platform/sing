package br.sing.reports.application;

import br.sing.exception.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportRegistry {

    private final Map<String, ReportDefinition> bySlug;

    public ReportRegistry(List<ReportDefinition> definitions) {
        this.bySlug = definitions.stream()
                .collect(Collectors.toMap(ReportDefinition::slug, Function.identity()));
    }

    public Collection<ReportDefinition> all() {
        return bySlug.values().stream()
                .sorted((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()))
                .toList();
    }

    public ReportDefinition require(String slug) {
        ReportDefinition def = bySlug.get(slug);
        if (def == null) {
            throw new EntityNotFoundException("Relatório não encontrado: " + slug);
        }
        return def;
    }
}
