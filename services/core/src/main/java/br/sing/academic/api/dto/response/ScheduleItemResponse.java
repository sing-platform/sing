package br.sing.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Item da Grade Horária")
public record ScheduleItemResponse(
    @Schema(description = "Dia da semana", example = "MONDAY")
    DayOfWeek dayOfWeek,
    @Schema(description = "Horário de início", example = "08:00:00")
    LocalTime startTime,
    @Schema(description = "Horário de término", example = "10:00:00")
    LocalTime endTime,
    @Schema(description = "ID da Turma", example = "1")
    Long schoolRoomId,
    @Schema(description = "Nome da Turma", example = "Robótica - Manhã")
    String schoolRoomName,
    @Schema(description = "Sala de aula", example = "Sala 01")
    String classroom,
    @Schema(description = "ID do Professor Titular", example = "2")
    Long teacherId,
    @Schema(description = "Nome do Professor Titular", example = "João Silva")
    String teacherName
) {}