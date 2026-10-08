package br.sing.academic.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "WorkshopUpdateRequest", description = "Corpo para **PUT /api/v1/workshops/{id}**.")
public record WorkshopUpdateRequest(
        @NotBlank(message = "Nome da oficina é obrigatório")
        @Size(max = 255, message = "Nome da oficina deve ter no máximo 255 caracteres")
        @Schema(description = "Novo nome da oficina (único entre ativas)", example = "Violão — Popular", requiredMode = Schema.RequiredMode.REQUIRED)
        String name
) {
}
