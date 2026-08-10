package br.cadi.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Turma com grade resolvida, oficina e professor titular (quando houver)")
public record SchoolRoomResponse(
        @Schema(description = "ID da turma", example = "10") Long id,
        @Schema(description = "Nome da turma", example = "Violão — Turma A") String name,
        @Schema(description = "Resumo textual da grade (ex.: Seg 08:00–09:30)", example = "Seg 08:00–09:30") String schedule,
        @Schema(description = "Intervalos persistidos da grade") List<ScheduleSlotResponse> scheduleSlots,
        @Schema(description = "Sala física", example = "Sala 12") String classroom,
        @Schema(description = "Capacidade", example = "20") Integer capacity,
        @Schema(description = "Turma ativa", example = "true") Boolean active,
        @Schema(description = "ID da oficina", example = "1") Long workshopId,
        @Schema(description = "Nome da oficina", example = "Violão") String workshopName,
        @Schema(description = "ID do professor titular", example = "5") Long teacherId,
        @Schema(description = "Nome do professor titular", example = "Maria Silva") String teacherName,
        @Schema(description = "Quantidade de matrículas ativas na turma", example = "12") int activeEnrollmentCount,
        @Schema(description = "Vagas restantes (capacity - matrículas ativas); null se capacidade não definida", example = "8")
        Integer vacancies
) {
}
