package br.cadi.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de operação sugerida ao confirmar a seleção de turmas")
public enum EnrollmentSuggestedAction {
    @Schema(description = "Nova(s) matrícula(s), sem remanejamento entre turmas da mesma oficina")
    ENROLL,
    @Schema(description = "Apenas remanejamento(s) dentro da(s) mesma(s) oficina(s)")
    TRANSFER,
    @Schema(description = "Combinação de nova matrícula e remanejamento")
    MIXED
}
