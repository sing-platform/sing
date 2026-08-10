package br.cadi.academic.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "WorkshopCreateRequest",
        description = "Corpo para **POST /api/v1/workshops**. Nome é normalizado com trim; se existir oficina inativa com o mesmo nome, ela é reativada."
)
public record WorkshopCreateRequest(
        @NotBlank(message = "Nome da oficina é obrigatório")
        @Size(max = 255, message = "Nome da oficina deve ter no máximo 255 caracteres")
        @Schema(description = "Nome da oficina (único entre ativas)", example = "Violão", requiredMode = Schema.RequiredMode.REQUIRED)
        String name
) {
}
