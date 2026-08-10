package br.cadi.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Oficina (cadastro administrativo)")
public record WorkshopResponse(
        @Schema(description = "ID da oficina", example = "1") Long id,
        @Schema(description = "Nome", example = "Violão") String name,
        @Schema(description = "Oficina ativa", example = "true") Boolean active
) {
}
