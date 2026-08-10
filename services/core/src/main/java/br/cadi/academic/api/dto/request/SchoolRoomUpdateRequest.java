package br.cadi.academic.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
        name = "SchoolRoomUpdateRequest",
        description = "Corpo para **PUT /api/v1/schoolrooms/{id}**: atualiza dados da turma, incluindo flag **active**."
)
public record SchoolRoomUpdateRequest(
        @NotBlank(message = "Nome da turma é obrigatório")
        @Size(max = 255, message = "Nome da turma deve ter no máximo 255 caracteres")
        @Schema(description = "Nome da turma", example = "Violão — Turma A", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotEmpty(message = "Informe ao menos um horário na grade")
        @Valid
        @Schema(description = "Grade semanal (ao menos um intervalo)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ScheduleSlotRequest> scheduleSlots,

        @NotBlank(message = "Sala é obrigatória")
        @Size(max = 50, message = "Sala deve ter no máximo 50 caracteres")
        @Schema(description = "Identificação da sala física", example = "Sala 12", requiredMode = Schema.RequiredMode.REQUIRED)
        String classroom,

        @NotNull(message = "Oficina é obrigatória")
        @Schema(description = "ID da oficina", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long workshopId,

        @Positive(message = "Capacidade deve ser maior que zero")
        @Schema(description = "Capacidade máxima de alunos", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer capacity,

        @Schema(description = "ID do professor titular (opcional)", example = "5")
        Long teacherId,

        @NotNull(message = "Status ativo é obrigatório")
        @Schema(description = "Turma ativa no sistema", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean active
) {
}
