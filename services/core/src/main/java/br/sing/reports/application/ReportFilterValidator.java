package br.sing.reports.application;

import br.sing.exception.BusinessException;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportFilterType;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportFilterValidator {

    private static final int MAX_PERIOD_DAYS = 366;

    public Map<String, Object> validateAndNormalize(
            ReportDefinition definition,
            Map<String, String> rawParams
    ) {
        Map<String, Object> normalized = new HashMap<>();
        LocalDate start = null;
        LocalDate end = null;

        for (ReportFilterSpec filter : definition.filters()) {
            String raw = rawParams.get(filter.name());
            if (raw == null || raw.isBlank()) {
                if (filter.required()) {
                    throw new BusinessException(
                            "Filtro obrigatório: " + filter.label(),
                            HttpStatus.BAD_REQUEST
                    );
                }
                if (filter.defaultValue() != null) {
                    normalized.put(filter.name(), filter.defaultValue());
                } else {
                    normalized.put(filter.name(), null);
                }
                continue;
            }
            Object value = parse(filter, raw.trim());
            normalized.put(filter.name(), value);
            if ("start".equals(filter.name()) && value instanceof LocalDate d) {
                start = d;
            }
            if ("end".equals(filter.name()) && value instanceof LocalDate d) {
                end = d;
            }
        }

        if (start == null && normalized.get("start") instanceof LocalDate d) {
            start = d;
        }
        if (end == null && normalized.get("end") instanceof LocalDate d) {
            end = d;
        }
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                throw new BusinessException("Data final deve ser >= data inicial.", HttpStatus.BAD_REQUEST);
            }
            long days = ChronoUnit.DAYS.between(start, end);
            if (days > MAX_PERIOD_DAYS) {
                throw new BusinessException(
                        "Período máximo de " + MAX_PERIOD_DAYS + " dias.",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
        return normalized;
    }

    private Object parse(ReportFilterSpec filter, String raw) {
        return switch (filter.type()) {
            case DATE -> LocalDate.parse(raw);
            case NUMBER -> {
                if (raw.contains(".")) {
                    yield Double.parseDouble(raw);
                }
                yield Long.parseLong(raw);
            }
            case TEXT, SELECT -> raw;
        };
    }

    public static Map<String, Object> periodDefaults() {
        LocalDate end = LocalDate.now();
        LocalDate start = LocalDate.of(end.getYear(), 1, 1);
        return Map.of("start", start, "end", end);
    }
}
