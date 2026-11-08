package br.sing.audience.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Público-alvo de uma publicação do mural")
public record AudienceRequest(

        @Schema(description = "Roles destinatárias (Secretaria/Admin)", example = "[\"ROLE_STUDENT\"]")
        Set<String> audienceRoles,

        @Schema(description = "IDs das turmas destinatárias", example = "[3]")
        Set<Long> audienceSchoolroomIds

) {}
