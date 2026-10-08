package br.sing.communication.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(
        name = "MuralPostCreateRequest",
        description = """
                Corpo para **POST /api/v1/mural**.

                ## Modos de público-alvo (mutuamente exclusivos por perfil)

                | Perfil | Campo obrigatório | Valores aceitos |
                |--------|-------------------|-----------------|
                | PROFESSOR | `audienceSchoolroomIds` | IDs das próprias turmas |
                | SECRETARY / ADMIN | `audienceRoles` | Qualquer role válida |

                **Professor** *não* pode usar `audienceRoles`; **Secretaria/Admin** *não* usam `audienceSchoolroomIds`.
                """
)
public record MuralPostCreateRequest(

        @NotBlank(message = "Título é obrigatório")
        @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
        @Schema(description = "Título da publicação", example = "Reunião pedagógica — próxima segunda-feira",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotBlank(message = "Conteúdo é obrigatório")
        @Schema(description = "Corpo da publicação (texto livre)",
                example = "Lembramos a todos que a reunião mensal será na segunda-feira às 17 h no auditório.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @Schema(description = "Roles destinatárias (Secretaria/Admin). Ex.: [\"ROLE_STUDENT\", \"ROLE_PARENT\"]",
                example = "[\"ROLE_STUDENT\", \"ROLE_PARENT\"]")
        Set<String> audienceRoles,

        @Schema(description = "IDs das turmas destinatárias (Professor). Ex.: [1, 3]",
                example = "[1, 3]")
        Set<Long> audienceSchoolroomIds

) {}
