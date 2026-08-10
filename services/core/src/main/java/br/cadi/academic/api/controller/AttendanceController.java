package br.cadi.academic.api.controller;

import br.cadi.academic.api.dto.response.*;
import br.cadi.academic.api.dto.request.*;
import br.cadi.academic.application.AttendanceService;
import br.cadi.academic.application.AttendanceSummaryService;
import br.cadi.common.application.AuditLogService;
import br.cadi.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance virtual call for lectures")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceSummaryService attendanceSummaryService;
    private final AuditLogService auditLogService;

    @GetMapping("/call/schoolroom/{schoolroomId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Iniciar chamada de uma turma (aula)",
            description = """
                    Retorna a lista de alunos matriculados na turma com o status atual de presença. "

                    - Se não houver chamada registrada naquele dia, os status virão nulos."
                    - Professor visualiza apenas suas próprias turmas. Gestão acessa qualquer turma."
                    - Não são permitidas datas futuras nem datas anteriores a 35 dias corridos. "
                    - Retorna erro se já houver outra chamada registrada para a mesma turma no mesmo dia.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chamada iniciada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para acessar esta turma",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe uma chamada para esta turma neste dia",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Nenhum aluno matriculado ativo na turma",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LectureCallResponse> startCall(
            @Parameter(description = "ID da turma", required = true, example = "1")
            @PathVariable Long schoolroomId,
            @Parameter(description = "Data da chamada (yyyy-MM-dd). Padrão: hoje", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date

    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.startCall(schoolroomId, targetDate));
    }

    @PostMapping("/call/batch")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Registrar presenças em lote",
            description = """
                    Corpo:  `schoolroomId`, `date` (yyyy-MM-dd), `topic` (opcional)
                    e lista `attendances` com `studentId`, `status` e `observation` (opcional). 
                    
                    Status válidos: `PRESENT`, `ABSENT`, e `JUSTIFIED`. "

                    - Chamadas já fechadas só podem ser alteradas se solicitada a reabertura do livro pelo usuário Manager ou Secretário.
                    - Retorna erro se já houver outra chamada registrada para a mesma turma no mesmo dia.
                    """

    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Presenças registradas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no corpo da requisição",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para registrar chamada nesta turma",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe uma chamada para esta turma neste dia",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Chamada já fechada ou aluno não matriculado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> registerBatch(@RequestBody @Valid BatchAttendanceRequest request) {
        attendanceService.registerBatch(request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/call/{lectureId}/close")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'SECRETARY', 'MANAGER')")
    @Operation(
            summary = "Fechar chamada virtual de uma aula",
            description = "Marca a chamada como fechada, bloqueando qualquer alteração posterior de presença. \n" +

                    "- A chamada é fechada após 2 dias do registro de frequência em lote e só pode ser reaberta para resolução de inconsistencias pelo Secretário ou Manager"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chamada fechada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Você não tem permissão para fechar chamadas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> closeCall(
            @Parameter(description = "ID da aula", required = true, example = "1")
            @PathVariable Long lectureId
    ) {
        attendanceService.closeCall(lectureId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/call/{lectureId}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER')")
    @Operation(
            summary = "Reabrir chamada de uma aula",
            description = "Reabre uma chamada previamente fechada, permitindo atualizações nos registros de presença. \n"
                    + "- Exclusivo para Admin, Gestor e Secretário. Útil para quando ocorre o fechamento automático "
                    + "porém se faz necessário tratar de alguma inconsistência no livro de chamada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chamada reaberta com sucesso"),
            @ApiResponse(responseCode = "403", description = "Apenas Admin ou Secretário podem reabrir chamadas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Chamada já está aberta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> reopenCall(
            @Parameter(description = "ID da aula", required = true, example = "1")
            @PathVariable Long lectureId
    ) {
        attendanceService.reopenCall(lectureId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary/my-schoolrooms")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(
            summary = "Resumo de chamadas das turmas do professor",
            description = "Retorna para cada turma do professor: aulas previstas, aulas registradas, "
                    + "% de chamadas registradas e % média de presença dos alunos no ano letivo atual."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Apenas professores podem acessar este endpoint")
    })
    public ResponseEntity<List<SchoolRoomAttendanceSummaryResponse>> getMySchoolRoomsSummary(
            Authentication authentication
    ) {
        return ResponseEntity.ok(attendanceSummaryService.getMySchoolRoomsSummary(authentication));
    }

    @GetMapping("/lectures/schoolroom/{schoolRoomId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Listar lectures de uma turma com status de chamada",
            description = """
                Retorna as aulas (lectures) de uma turma em um período,
                com status de chamada (registrada ou não, aberta ou fechada).

                **GET** `/api/v1/attendance/lectures/schoolroom/{schoolRoomId}`

                ## Parâmetros de path
                | Parâmetro | Descrição |
                |-----------|-----------|
                | schoolRoomId | ID da turma |

                ## Parâmetros de query
                | Parâmetro | Padrão | Descrição |
                |-----------|--------|-----------|
                | start | início do ano | Data inicial (yyyy-MM-dd) |
                | end | hoje | Data final (yyyy-MM-dd) |
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de lectures com status"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para esta turma"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    public ResponseEntity<List<LectureSummaryResponse>> getLecturesSummary(
            @PathVariable Long schoolRoomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDate s = start != null ? start : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate e = end   != null ? end   : LocalDate.now();
        return ResponseEntity.ok(attendanceSummaryService.getLecturesSummary(schoolRoomId, s, e));
    }

    @GetMapping("/{attendanceId}/last-change")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Última alteração de uma presença",
            description = "Retorna o registro de auditoria da última alteração feita em uma presença (quem alterou e quando)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro de auditoria encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este registro",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Nenhuma alteração registrada para esta presença",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AttendanceAuditResponse> getLastAttendanceChange(
            @Parameter(description = "ID da presença", example = "1", required = true)
            @PathVariable Long attendanceId
    ) {
        return auditLogService
                .findLastAttendanceChange(attendanceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}