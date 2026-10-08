package br.sing.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Turmas ativas e alunos matriculados por turma (grade admin)")
public record AdminScheduleBoardResponse(
        @Schema(description = "Turmas ativas com grade e metadados")
        List<SchoolRoomResponse> rooms,
        @Schema(description = "Alunos por ID da turma")
        Map<Long, List<SchoolRoomStudentResponse>> studentsByRoomId
) {
}
