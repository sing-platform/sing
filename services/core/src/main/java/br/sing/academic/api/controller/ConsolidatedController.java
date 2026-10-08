package br.sing.academic.api.controller;

import br.sing.academic.api.dto.response.ConsolidatedResponse;
import br.sing.academic.application.ConsolidatedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/consolidated")
@RequiredArgsConstructor
@Tag(name = "Consolidated Attendance", description = "Consulta consolidada de frequência e grade horária")
public class ConsolidatedController {

    private final ConsolidatedQueryService consolidatedQueryService;

    @GetMapping("/attendance-schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR', 'STUDENT')")
    @Operation(
            summary = "Consulta consolidada de frequência e grade",
            description = """
            Retorna frequência e grade horária no mesmo payload, com filtros opcionais.
            O escopo é aplicado automaticamente por perfil:
            - **Gestão (ADMIN/SECRETARY/MANAGER):** acessa qualquer turma ou aluno.
            - **Professor:** acessa apenas suas próprias turmas.
            - **Aluno:** acessa apenas sua própria frequência.

            Ao menos um filtro (`schoolRoomId` ou `studentId`) deve ser informado para perfis não-gestão.

            Quando um dos blocos (grade ou frequência) não tiver dados, ele retorna com
            `available: false` e `unavailableReason` explicando o motivo, sem interromper a consulta.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão ou filtros fora do escopo do perfil",
                    content = @Content(schema = @Schema(implementation = br.sing.academic.api.dto.response.ErrorResponse.class))),
    })
    public ResponseEntity<ConsolidatedResponse> getConsolidated(
            @Parameter(description = "ID da turma (opcional)")
            @RequestParam(required = false) Long schoolRoomId,

            @Parameter(description = "ID do aluno (opcional)")
            @RequestParam(required = false) Long studentId,

            @Parameter(description = "Data início do período (yyyy-MM-dd). Padrão: 30 dias atrás")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Data fim do período (yyyy-MM-dd). Padrão: hoje")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Filtro de turno: MANHA, TARDE ou NOITE (opcional)")
            @RequestParam(required = false) String turno,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                consolidatedQueryService.getConsolidated(
                        schoolRoomId, studentId, startDate, endDate, turno, authentication
                )
        );
    }
}