package br.sing.academic.api.dto.response;

import br.sing.academic.domain.enums.DescriptiveReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DescriptiveReportResponse(
        @Schema(description = "ID do parecer", example = "100")
        Long id,
        @Schema(description = "ID do aluno", example = "20")
        Long studentId,
        @Schema(description = "Nome do aluno", example = "Aluno B")
        String studentName,
        @Schema(description = "ID da turma", example = "40")
        Long schoolRoomId,
        @Schema(description = "Nome da turma", example = "Turma 6A")
        String schoolRoomName,
        @Schema(description = "ID da oficina/componente curricular", example = "30")
        Long workshopId,
        @Schema(description = "Nome da oficina/componente curricular", example = "Robótica")
        String workshopName,
        @Schema(description = "ID do autor do parecer", example = "10")
        Long authorId,
        @Schema(description = "Nome do autor do parecer", example = "Professor A")
        String authorName,
        @Schema(description = "Conteúdo do parecer")
        String content,
        @Schema(description = "Estado do parecer", example = "DRAFT")
        DescriptiveReportStatus status,
        @Schema(description = "Data de criação", example = "2026-05-06T10:30:00")
        LocalDateTime createdAt,
        @Schema(description = "Data da última atualização", example = "2026-05-06T10:35:00")
        LocalDateTime updatedAt,
        @Schema(description = "Data de publicação (quando publicado)", example = "2026-05-06T10:36:00")
        LocalDateTime publishedAt
) {
}
