package br.sing.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Turma vinculada à oficina no impacto da desativação")
public record SchoolRoomDeactivationImpactRow(
        @Schema(example = "10") Long id,
        @Schema(example = "Violão — Turma A") String name,
        @Schema(example = "true") boolean active,
        @Schema(description = "Se há matrículas ativas na turma", example = "false") boolean hasActiveEnrollments
) {
}
