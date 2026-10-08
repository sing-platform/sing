package br.sing.people.api.dto.request;

import br.sing.people.domain.enums.Shift;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        name = "StudentPreRegisterCreateRequest",
        description = "Corpo para **POST /api/v1/student-pre-registers**. Cadastra candidato na lista de espera com oficina e turno de interesse."
)
public record StudentPreRegisterCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        @Schema(description = "Candidate full name", example = "Maria Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must be at most 255 characters")
        @Schema(description = "Contact email", example = "maria@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "Phone is required")
        @Size(max = 50, message = "Phone must be at most 50 characters")
        @Schema(description = "Contact phone", example = "11999998888", requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @NotNull(message = "Workshop of interest id is required")
        @Schema(description = "Workshop of interest id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long workshopId,

        @NotNull(message = "Shift is required")
        @Schema(description = "Preferred shift", example = "MORNING", requiredMode = Schema.RequiredMode.REQUIRED)
        Shift shift
) {
}
