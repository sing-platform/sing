package br.sing.reports.application;

import br.sing.reports.api.dto.ReportFilterResponse;
import br.sing.reports.api.dto.ReportMetadataResponse;
import br.sing.reports.api.dto.ReportResultResponse;
import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRegistry registry;
    private final ReportFilterValidator filterValidator;

    @Transactional(readOnly = true)
    public List<ReportMetadataResponse> listMetadata() {
        return registry.all().stream()
                .filter(this::canAccessDefinition)
                .map(this::toMetadata)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResultResponse run(String slug, Map<String, String> rawParams) {
        ReportDefinition definition = registry.require(slug);
        if (!canAccessDefinition(definition)) {
            throw new br.sing.exception.BusinessException("Sem permissão para este relatório", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        Map<String, Object> filters = filterValidator.validateAndNormalize(definition, rawParams);
        ReportResult result = definition.execute(filters);
        return toResponse(definition, result);
    }

    @Transactional(readOnly = true)
    public ReportResult runForExport(String slug, Map<String, String> rawParams) {
        ReportDefinition definition = registry.require(slug);
        if (!canAccessDefinition(definition)) {
            throw new br.sing.exception.BusinessException("Sem permissão para este relatório", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        Map<String, Object> filters = filterValidator.validateAndNormalize(definition, rawParams);
        return definition.execute(filters);
    }

    private boolean canAccessDefinition(ReportDefinition definition) {
        if (!"Financeiro".equalsIgnoreCase(definition.category())) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_MANAGER".equals(a));
    }

    private ReportMetadataResponse toMetadata(ReportDefinition definition) {
        List<ReportFilterResponse> filters = definition.filters().stream()
                .map(f -> new ReportFilterResponse(
                        f.name(),
                        f.label(),
                        f.type().name(),
                        f.required(),
                        f.defaultValue(),
                        f.options(),
                        f.optionsSource()
                ))
                .toList();
        return new ReportMetadataResponse(
                definition.slug(),
                definition.displayName(),
                definition.description(),
                definition.category(),
                filters
        );
    }

    private ReportResultResponse toResponse(ReportDefinition definition, ReportResult result) {
        List<String> columns = result.columns().stream().map(ReportColumn::label).toList();
        List<String> keys = result.columns().stream().map(ReportColumn::key).toList();
        return new ReportResultResponse(
                definition.slug(),
                definition.displayName(),
                columns,
                keys,
                result.rows(),
                result.totals(),
                result.totalRows()
        );
    }
}
