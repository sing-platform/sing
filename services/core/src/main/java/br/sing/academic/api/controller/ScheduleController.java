package br.sing.academic.api.controller;

import br.sing.academic.api.dto.response.ScheduleItemResponse;
import br.sing.academic.application.ScheduleQueryService;
import br.sing.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Consultas e Grade Horária (Itinerário)")
public class ScheduleController {

    private final ScheduleQueryService scheduleQueryService;

    @Operation(summary = "Consulta a grade horária", description = "Retorna a grade de aulas de acordo com o perfil do usuário logado, filtrando opcionalmente por turma, professor, aluno e turno.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade horária retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para a consulta solicitada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<ScheduleItemResponse>> getSchedules(
            @Parameter(description = "Filtrar por ID da turma", example = "1")
            @RequestParam(required = false) Long schoolRoomId,
            @Parameter(description = "Filtrar por ID do professor", example = "1")
            @RequestParam(required = false) Long teacherId,
            @Parameter(description = "Filtrar por ID do aluno", example = "1")
            @RequestParam(required = false) Long studentId,
            @Parameter(description = "Filtrar por turno (ex.: MANHA, TARDE, NOITE)", example = "MANHA")
            @RequestParam(required = false) String turno,
            Authentication authentication
    ) {
        List<ScheduleItemResponse> schedules = scheduleQueryService.getSchedule(
                schoolRoomId, teacherId, studentId, turno, authentication
        );
        return ResponseEntity.ok(schedules);
    }
}