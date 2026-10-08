package br.sing.academic.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Builder
@Schema(description = "Intervalo da grade persistido")
public record ScheduleSlotResponse(
        @Schema(example = "100") Long id,

        @Schema(example = "MONDAY") DayOfWeek dayOfWeek,
        @JsonFormat(pattern = "HH:mm")
        @Schema(example = "08:00") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        @Schema(example = "09:30") LocalTime endTime
) {
}
