package br.sing.academic.api.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Intervalo semanal na grade (dia + início/fim em HH:mm)")
public record ScheduleSlotRequest(
        @NotNull(message = "Dia da semana é obrigatório")
        @Schema(description = "Dia da semana (enum Jackson: nome em inglês)", example = "MONDAY", requiredMode = Schema.RequiredMode.REQUIRED)
        DayOfWeek dayOfWeek,

        @NotNull(message = "Horário de início é obrigatório")
        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "Início (formato HH:mm)", example = "08:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime startTime,

        @NotNull(message = "Horário de fim é obrigatório")
        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "Fim (formato HH:mm)", example = "09:30", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime endTime
) {
}
