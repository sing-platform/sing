package br.sing.academic.api.controller;

import br.sing.academic.api.controller.examples.AcademicApiExamples;
import br.sing.academic.api.dto.request.DescriptiveReportUpsertRequest;
import br.sing.academic.api.dto.response.DescriptiveReportResponse;
import br.sing.academic.application.DescriptiveReportService;
import br.sing.academic.domain.enums.DescriptiveReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/descriptive-reports")
@RequiredArgsConstructor
@Tag(name = "Descriptive Reports", description = "Pareceres pedagógicos por aluno e turma")
public class DescriptiveReportController {

    private final DescriptiveReportService descriptiveReportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR')")
    @Operation(
            summary = "Listar pareceres descritivos",
            description = """
                    Lista pareceres com filtros opcionais por turma, aluno, oficina e status.
                    Professor enxerga apenas seu próprio escopo; gestão (`ADMIN`/`SECRETARY`/`MANAGER`) enxerga escopo administrativo.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pareceres",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.DESCRIPTIVE_REPORT_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_DESCRIPTIVE_REPORTS)))
    })
    public ResponseEntity<List<DescriptiveReportResponse>> list(
            @Parameter(description = "ID da turma", example = "40")
            @RequestParam(required = false) Long schoolRoomId,
            @Parameter(description = "ID do aluno", example = "20")
            @RequestParam(required = false) Long studentId,
            @Parameter(description = "Status do parecer", example = "DRAFT")
            @RequestParam(required = false) DescriptiveReportStatus status,
            @Parameter(description = "ID da oficina/componente curricular", example = "30")
            @RequestParam(required = false) Long workshopId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(descriptiveReportService.list(
                schoolRoomId, studentId, status, workshopId, authentication
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR')")
    @Operation(summary = "Detalhar parecer descritivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parecer encontrado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.DESCRIPTIVE_REPORT_RESPONSE_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "404", description = "Parecer não encontrado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_DESCRIPTIVE_REPORT)))
    })
    public ResponseEntity<DescriptiveReportResponse> getById(
            @Parameter(description = "ID do parecer", example = "100")
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(descriptiveReportService.getById(id, authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR')")
    @Operation(
            summary = "Criar parecer descritivo",
            description = "Cria parecer para aluno e turma. Requer matrícula ativa do aluno na turma (um parecer por aluno/turma)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parecer criado",
                    content = @Content(examples = {
                            @ExampleObject(name = "request", value = AcademicApiExamples.DESCRIPTIVE_REPORT_REQUEST_JSON),
                            @ExampleObject(name = "response", value = AcademicApiExamples.DESCRIPTIVE_REPORT_RESPONSE_JSON)
                    })),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "409", description = "Conflito de regra de negócio",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_DESCRIPTIVE_REPORT)))
    })
    public ResponseEntity<DescriptiveReportResponse> create(
            @RequestBody @Valid DescriptiveReportUpsertRequest request,
            Authentication authentication
    ) {
        DescriptiveReportResponse response = descriptiveReportService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR')")
    @Operation(
            summary = "Editar parecer descritivo",
            description = "Atualiza conteúdo de parecer em rascunho. Parecer publicado/fechado não aceita edição."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parecer atualizado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.DESCRIPTIVE_REPORT_RESPONSE_JSON))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "404", description = "Parecer não encontrado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_DESCRIPTIVE_REPORT))),
            @ApiResponse(responseCode = "409", description = "Conflito de regra de negócio",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_DESCRIPTIVE_REPORT)))
    })
    public ResponseEntity<DescriptiveReportResponse> update(
            @Parameter(description = "ID do parecer", example = "100")
            @PathVariable Long id,
            @RequestBody @Valid DescriptiveReportUpsertRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(descriptiveReportService.update(id, request, authentication));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR')")
    @Operation(summary = "Publicar parecer descritivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parecer publicado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.DESCRIPTIVE_REPORT_RESPONSE_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "404", description = "Parecer não encontrado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_DESCRIPTIVE_REPORT))),
            @ApiResponse(responseCode = "409", description = "Conflito de regra de negócio",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_DESCRIPTIVE_REPORT)))
    })
    public ResponseEntity<DescriptiveReportResponse> publish(
            @Parameter(description = "ID do parecer", example = "100")
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(descriptiveReportService.publish(id, authentication));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR')")
    @Operation(summary = "Fechar parecer descritivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parecer fechado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.DESCRIPTIVE_REPORT_RESPONSE_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "404", description = "Parecer não encontrado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_DESCRIPTIVE_REPORT))),
            @ApiResponse(responseCode = "409", description = "Conflito de regra de negócio",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_DESCRIPTIVE_REPORT)))
    })
    public ResponseEntity<DescriptiveReportResponse> close(
            @Parameter(description = "ID do parecer", example = "100")
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(descriptiveReportService.close(id, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR')")
    @Operation(summary = "Excluir parecer em rascunho")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Parecer excluído"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de acesso",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_DESCRIPTIVE_REPORTS))),
            @ApiResponse(responseCode = "404", description = "Parecer não encontrado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_DESCRIPTIVE_REPORT))),
            @ApiResponse(responseCode = "409", description = "Conflito de regra de negócio",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_DESCRIPTIVE_REPORT)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do parecer", example = "100")
            @PathVariable Long id,
            Authentication authentication
    ) {
        descriptiveReportService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
