package br.sing.iam.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {

    @Schema(description = "E-mail do usuário (seed dev: admin@sing.local, teacher@sing.local, student@sing.local, etc.)",
            example = "teacher@sing.local")
    @NotBlank(message = "Email obrigatorio")
    private String email;


    @Schema(description = "senha do usuario", example = "SING@123")
    @NotBlank(message = "Senha obrigatoria")
    private String password;
}
