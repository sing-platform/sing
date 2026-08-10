package br.cadi.communication.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(
        name = "MuralPostUpdateRequest",
        description = """
                Corpo para **PUT /api/v1/mural/{id}**.
                Substitui título, conteúdo e público-alvo integralmente.

                Mesmas regras de `MuralPostCreateRequest`:
                - **Professor** usa `audienceSchoolroomIds` (suas próprias turmas).
                - **Secretaria/Admin** usam `audienceRoles`.
                """
)
public record MuralPostUpdateRequest(

        @NotBlank(message = "Título é obrigatório")
        @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
        @Schema(description = "Novo título", example = "Reunião pedagógica — data alterada",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotBlank(message = "Conteúdo é obrigatório")
        @Schema(description = "Novo conteúdo",
                example = "A reunião foi remarcada para terça-feira às 17 h.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @Schema(description = "Roles destinatárias (Secretaria/Admin). Ex.: [\"ROLE_STUDENT\", \"ROLE_PARENT\"]",
                example = "[\"ROLE_STUDENT\", \"ROLE_PARENT\"]")
        Set<String> audienceRoles,

        @Schema(description = "IDs das turmas destinatárias (Professor). Ex.: [1, 3]",
                example = "[1, 3]")
        Set<Long> audienceSchoolroomIds

) {}
