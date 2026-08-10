package br.cadi.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Problema encontrado na validação de preview de matrícula")
public record EnrollmentPreviewIssue(
        @Schema(description = "Código estável para o cliente", example = "CAPACITY_EXCEEDED")
        String code,
        @Schema(description = "Mensagem legível")
        String message,
        @Schema(description = "HTTP status equivalente", example = "400")
        int status
) {
}
