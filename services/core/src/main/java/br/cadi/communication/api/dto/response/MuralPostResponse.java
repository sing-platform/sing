package br.cadi.communication.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Schema(description = "Publicação do mural institucional")
public record MuralPostResponse(

        @Schema(description = "ID da publicação", example = "1")
        Long id,

        @Schema(description = "Título", example = "Reunião pedagógica — próxima segunda-feira")
        String title,

        @Schema(description = "Conteúdo da publicação")
        String content,

        @Schema(description = "Nome do autor", example = "Ricardo Mendes")
        String authorName,

        @Schema(description = "Data de criação")
        LocalDateTime createdAt,

        @Schema(description = "Data da última edição (null se nunca editado)")
        LocalDateTime updatedAt,

        @Schema(description = "Roles que podem visualizar esta publicação (targeting por role)",
                example = "[\"ROLE_STUDENT\", \"ROLE_PARENT\"]")
        Set<String> audienceRoles,

        @Schema(description = "IDs das turmas destinatárias (targeting por turma — Professor)",
                example = "[1, 3]")
        Set<Long> audienceSchoolroomIds,

        @Schema(description = "Indica se o usuário autenticado já visualizou esta publicação. " +
                "Sempre false para Admin/Secretaria (eles não têm view tracking).")
        boolean viewed

) {}
