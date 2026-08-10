package br.cadi.people.api.controller;

import br.cadi.exception.ApiError;
import br.cadi.people.api.dto.response.GuardianResponse;
import br.cadi.people.api.dto.request.GuardianRequest;
import br.cadi.people.application.GuardianService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/guardians")
@RequiredArgsConstructor
@Tag(name = "Guardian", description = "Endpoints for guardian management")
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping
    @Operation(
            summary = "Cadastrar novo responsável",
            description = """
                Cadastra um novo responsável (pai, mãe, etc.) no sistema. 
                Este registro pode ser utilizado posteriormente para vincular a um ou mais alunos.
                
                ## Restrições
                - Requer autenticação com token de **ROLE_ADMIN** ou **ROLE_SECRETARY**.
                - O **CPF** deve ser único e conter apenas os 11 dígitos numéricos.
                
                ## Regras e Vínculos (familyTie)
                | Valor | Descrição |
                |-------|-----------|
                | **MOTHER** | Mãe do aluno |
                | **FATHER** | Pai do aluno |
                | **GRANDPARENT** | Avô ou Avó |
                | **OTHER** | Outro tipo de parentesco ou tutor legal |
                
                ## Observações
                - **isPrimary:** Define se este é o contato principal para emergências e recados.
                - **Address:** O endereço é obrigatório.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Responsável criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos dados enviados"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "409", description = "Conflito: CPF ou E-mail já cadastrados")
    })
    public ResponseEntity<GuardianResponse> create(@RequestBody @Valid GuardianRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardianService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Lists all guardians")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de responsáveis retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<GuardianResponse>> findAll() {
        return ResponseEntity.ok(guardianService.findAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a guardian's information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Responsável atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos dados enviados",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Responsável não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<GuardianResponse> update(
            @Parameter(description = "ID do responsável", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody @Valid GuardianRequest dto) {
        return ResponseEntity.ok(guardianService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a guardian and their login user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Responsável excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Responsável não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do responsável", example = "1", required = true)
            @PathVariable Long id) {
        guardianService.delete(id);
        return ResponseEntity.noContent().build();
    }
}