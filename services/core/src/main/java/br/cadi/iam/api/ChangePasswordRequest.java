package br.cadi.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, message = "Nova senha deve ter ao menos 8 caracteres")
        String newPassword,

        @NotBlank(message = "Confirmação da senha é obrigatória")
        String confirmPassword
) {
}
