package br.sing.academic.api.controller;

import br.sing.academic.api.dto.request.EnrollmentRequest;
import br.sing.academic.api.dto.response.EnrollmentPreviewResponse;
import br.sing.academic.application.EnrollmentService;
import br.sing.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment", description = "Student enrollment")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Pré-validar matrícula sem gravar",
            description = "Mesmo corpo do POST de matrícula. Retorna `ok`, lista de `issues` (vazia se válido) e `suggestedAction` para o fluxo na UI."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pré-validação concluída"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN ou SECRETARY)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EnrollmentPreviewResponse> preview(@RequestBody @Valid EnrollmentRequest request) {
        return ResponseEntity.ok(enrollmentService.previewEnrollment(request));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Matricular aluno em uma ou duas turmas",
            description = "Corpo: `studentId` e `schoolRoomIds` (lista com 1 ou 2 IDs). "
                    + "Com duas turmas, o backend valida tudo antes de gravar (sem matrícula parcial)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aluno matriculado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN ou SECRETARY)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Aluno ou turma não encontrados",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada (ex.: turma sem vaga ou matrícula já existente)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<String> enroll(@RequestBody @Valid EnrollmentRequest request) {
        enrollmentService.enrollStudent(request);
        String body = request.schoolRoomIds().size() == 1
                ? "Aluno matriculado com sucesso!"
                : "Matrículas concluídas com sucesso!";
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @DeleteMapping("/student/{studentId}/room/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Cancelar matrícula de um aluno em uma turma",
            description = "Remove o vínculo de matrícula entre o aluno e a turma informados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Matrícula cancelada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN ou SECRETARY)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> unenroll(
            @Parameter(description = "ID do aluno", example = "1", required = true)
            @PathVariable Long studentId,
            @Parameter(description = "ID da turma", example = "1", required = true)
            @PathVariable Long roomId) {
        enrollmentService.unenrollStudent(studentId, roomId);
        return ResponseEntity.noContent().build();
    }
}
