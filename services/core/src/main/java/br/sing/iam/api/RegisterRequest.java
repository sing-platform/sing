package br.sing.iam.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para registro público de usuário")
public record RegisterRequest(
        @Schema(description = "nome de usuario", example = "string")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        String username,

        @Schema(
                description = "Email do usuário",
                example = "string@email.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        @Schema(description = "senha para login", example = "string")
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password should have at least 6 characters")
        String password
) {
}
