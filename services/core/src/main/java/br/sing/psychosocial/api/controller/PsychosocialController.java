package br.sing.psychosocial.api.controller;

import br.sing.exception.ApiError;
import br.sing.iam.domain.User;
import br.sing.psychosocial.api.dto.request.PatientRecordRequest;
import br.sing.psychosocial.api.dto.response.PatientClinicalResponse;
import br.sing.psychosocial.api.dto.response.PatientRecordResponse;
import br.sing.psychosocial.api.dto.response.PatientSummaryResponse;
import br.sing.psychosocial.api.dto.response.StudentSearchResponse;
import br.sing.psychosocial.application.PsychosocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints do psicólogo: gestão dos próprios pacientes (vínculo), visão de
 * dados clínicos (somente leitura) e anotações terapêuticas (privadas do autor).
 */
@RestController
@RequestMapping("/api/psychosocial")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PSYCHOLOGIST')")
@Tag(name = "Psicossocial", description = "Pacientes, dados clínicos e anotações do psicólogo")
public class PsychosocialController {

    private final PsychosocialService service;

    // ── Pacientes / vínculo ─────────────────────────────────────────────────

    @GetMapping("/patients")
    @Operation(summary = "Listar meus pacientes", description = "Alunos vinculados ao psicólogo autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pacientes retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer PSYCHOLOGIST)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PatientSummaryResponse>> listMyPatients(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.listMyPatients(user.getId()));
    }

    @GetMapping("/patients/search")
    @Operation(summary = "Buscar alunos para vincular",
            description = "Lista alunos por nome; o campo 'linked' indica se já é paciente do psicólogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de alunos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer PSYCHOLOGIST)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<StudentSearchResponse>> searchStudents(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "Termo de busca pelo nome do aluno", example = "Maria")
            @RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(service.searchStudents(user.getId(), query));
    }

    @PostMapping("/patients/{studentId}")
    @Operation(summary = "Vincular paciente", description = "Auto-vincula um aluno como paciente do psicólogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente vinculado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer PSYCHOLOGIST)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientSummaryResponse> linkPatient(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID do aluno", example = "1", required = true)
            @PathVariable Long studentId) {
        return ResponseEntity.status(201).body(service.linkPatient(user.getId(), studentId));
    }

    @DeleteMapping("/patients/{studentId}")
    @Operation(summary = "Desvincular paciente", description = "Remove o vínculo do aluno com o psicólogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vínculo removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer PSYCHOLOGIST)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Vínculo não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> unlinkPatient(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID do aluno", example = "1", required = true)
            @PathVariable Long studentId) {
        service.unlinkPatient(user.getId(), studentId);
        return ResponseEntity.noContent().build();
    }

    // ── Dados clínicos (somente leitura) ────────────────────────────────────

    @GetMapping("/patients/{patientId}/clinical")
    @Operation(summary = "Dados clínicos do paciente",
            description = "Retorna os dados clínicos (somente leitura) de um paciente vinculado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados clínicos retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer PSYCHOLOGIST)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado ou não vinculado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientClinicalResponse> getPatientClinical(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long patientId) {
        return ResponseEntity.ok(service.getPatientClinical(user.getId(), patientId));
    }

    // ── Anotações terapêuticas ──────────────────────────────────────────────

    @GetMapping("/patients/{patientId}/records")
    @Operation(summary = "Listar anotações do paciente",
            description = "Histórico de anotações do psicólogo autor para o paciente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de anotações retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer PSYCHOLOGIST)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado ou não vinculado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PatientRecordResponse>> listRecords(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long patientId) {
        return ResponseEntity.ok(service.listRecords(user.getId(), patientId));
    }

    @PostMapping("/patients/{patientId}/records")
    @Operation(summary = "Criar anotação", description = "Adiciona uma anotação terapêutica para o paciente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Anotação criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer PSYCHOLOGIST)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado ou não vinculado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientRecordResponse> addRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long patientId,
            @Valid @RequestBody PatientRecordRequest request) {
        return ResponseEntity.status(201).body(service.addRecord(user.getId(), patientId, request));
    }

    @PutMapping("/records/{recordId}")
    @Operation(summary = "Editar anotação", description = "Atualiza uma anotação do próprio psicólogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Anotação atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (anotação de outro psicólogo)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Anotação não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientRecordResponse> updateRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID da anotação", example = "1", required = true)
            @PathVariable Long recordId,
            @Valid @RequestBody PatientRecordRequest request) {
        return ResponseEntity.ok(service.updateRecord(user.getId(), recordId, request));
    }

    @DeleteMapping("/records/{recordId}")
    @Operation(summary = "Excluir anotação", description = "Soft delete de uma anotação do próprio psicólogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Anotação excluída com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (anotação de outro psicólogo)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Anotação não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> deleteRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID da anotação", example = "1", required = true)
            @PathVariable Long recordId) {
        service.deleteRecord(user.getId(), recordId);
        return ResponseEntity.noContent().build();
    }
}
