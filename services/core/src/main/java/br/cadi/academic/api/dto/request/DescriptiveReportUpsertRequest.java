package br.cadi.academic.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DescriptiveReportUpsertRequest(
        @Schema(description = "ID do aluno", example = "20")
        @NotNull Long studentId,
        @Schema(description = "ID da turma", example = "40")
        @NotNull Long schoolRoomId,
        @Schema(description = "Texto do parecer", example = "Aluno apresentou evolução consistente em leitura e participação.")
        @NotBlank @Size(max = 4000) String content
) {
}
