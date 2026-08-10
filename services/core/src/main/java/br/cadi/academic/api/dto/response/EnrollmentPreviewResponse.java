package br.cadi.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado da validação de matrícula sem persistência")
public record EnrollmentPreviewResponse(
        @Schema(description = "true se a matrícula pode ser confirmada com os mesmos dados")
        boolean ok,
        @Schema(description = "Detalhes quando ok=false")
        List<EnrollmentPreviewIssue> issues,
        @Schema(description = "Sugestão de fluxo na UI; preenchido quando ok=true")
        EnrollmentSuggestedAction suggestedAction
) {
}
