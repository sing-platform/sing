package br.sing.iam.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para registro de usuário")
public record RegisterRequestAdmin(
        @Schema(example = "joao123")
        @NotBlank
        String username,

        @Schema(example = "joao@email.com")
        @Email
        @NotBlank
        String email,

        @Schema(example = "Senha@123")
        @NotBlank
        String password,

        @Schema(
                example = "ROLE_PROFESSOR",
                description = "Role que esse usuário possui; deve existir no banco de dados"
        )
        @NotBlank
        String role
) {
}
