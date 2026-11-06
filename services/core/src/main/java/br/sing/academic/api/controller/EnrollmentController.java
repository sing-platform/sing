package br.sing.academic.api.controller;

import br.sing.academic.api.dto.request.EnrollmentRequest;
import br.sing.academic.api.dto.response.EnrollmentPreviewResponse;
import br.sing.academic.api.dto.response.IntegracaoMatriculaResponse;
import br.sing.academic.api.dto.response.MatriculasCriadasResponse;
import br.sing.academic.application.EnrollmentService;
import br.sing.academic.application.MatriculaIntegracaoService;
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
    private final MatriculaIntegracaoService matriculaIntegracaoService;

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
                    + "Com duas turmas, o backend valida tudo antes de gravar (sem matrícula parcial). "
                    + "A resposta traz o id e o eventId de cada matrícula criada, com publicação e cobrança pendentes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Matrícula gravada; publicação e cobrança ficam pendentes"),
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
    public ResponseEntity<MatriculasCriadasResponse> enroll(@RequestBody @Valid EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollStudent(request));
    }

    @GetMapping("/{matriculaId}/integracoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Consultar andamento da publicação e da cobrança",
            description = "Status possíveis: pendente, concluida, esgotada. Cobrança também pode ser conflito."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Andamento da matrícula"),
            @ApiResponse(responseCode = "404", description = "Matrícula sem pendência de integração",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public IntegracaoMatriculaResponse integracoes(
            @Parameter(description = "ID da matrícula", example = "9001", required = true)
            @PathVariable Long matriculaId) {
        return matriculaIntegracaoService.consultar(matriculaId);
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
