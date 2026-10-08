package br.sing.people.api.controller;

import br.sing.people.api.dto.request.CollaboratorCreateRequest;
import br.sing.people.api.dto.request.CollaboratorUpdateRequest;
import br.sing.people.api.dto.response.CollaboratorDetailResponse;
import br.sing.people.api.dto.response.CollaboratorStatsResponse;
import br.sing.people.application.CollaboratorService;
import br.sing.people.domain.enums.CollaboratorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/collaborators")
@Tag( name = "Collaborator", description = "Endpoints responsáveis pelo gerenciamento de colaboradores " +
                "(professores, secretárias, staff e demais funcionários).")
@SecurityRequirement(name = "bearerAuth")
public class CollaboratorController {

    private final CollaboratorService collaboratorService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation( summary = "Listar colaboradores", description = "Retorna a lista de colaboradores cadastrados, podendo filtrar por status ativo e tipo.")
    @ApiResponses(value = {
            @ApiResponse( responseCode = "200", description = "Lista de colaboradores retornada com sucesso"),
            @ApiResponse( responseCode = "403", description = "Usuário sem permissão para acessar o recurso")
    })
    public ResponseEntity<List<CollaboratorDetailResponse>> listAll(

            @Parameter( description = "Define se apenas colaboradores ativos devem ser retornados", example = "true")
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @Parameter( description = "Filtra colaboradores por tipo",  example = "TEACHER")
            @RequestParam(required = false) CollaboratorType type,
            @Parameter(description = "Quando true com type=TEACHER, inclui titulares elegíveis (tipo TEACHER ou role ROLE_PROFESSOR)")
            @RequestParam(defaultValue = "false") boolean eligibleForSchoolRoom
    ) {
        return ResponseEntity.ok(collaboratorService.listAll(activeOnly, type, eligibleForSchoolRoom));
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation( summary = "Obter estatísticas de colaboradores", description = "Retorna estatísticas agrupadas por tipo de colaborador.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estatísticas retornadas com sucesso",
                    content = @Content(schema = @Schema(implementation = CollaboratorStatsResponse.class))),
            @ApiResponse( responseCode = "403", description = "Usuário sem permissão")
    })
    public ResponseEntity<CollaboratorStatsResponse> stats() {
        return ResponseEntity.ok(collaboratorService.getStats());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation( summary = "Buscar colaborador por ID", description = "Retorna os detalhes de um colaborador específico.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Colaborador encontrado com sucesso",
                    content = @Content(schema = @Schema(implementation = CollaboratorDetailResponse.class)) ),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado" ),
            @ApiResponse( responseCode = "403", description = "Usuário sem permissão")
    })
    public ResponseEntity<CollaboratorDetailResponse> getById(
            @Parameter( description = "ID do colaborador", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(collaboratorService.getById(id));
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE )
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation( summary = "Cadastrar colaborador", description = "Realiza o cadastro de um novo colaborador no sistema.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Colaborador criado com sucesso",
                    content = @Content(schema = @Schema(implementation = CollaboratorDetailResponse.class))),
            @ApiResponse( responseCode = "400", description = "Dados inválidos" ),
            @ApiResponse( responseCode = "403", description = "Acesso negado"),
            @ApiResponse( responseCode = "409", description = "CPF ou e-mail já cadastrado")
    })
    public ResponseEntity<CollaboratorDetailResponse> create(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados necessários para criação do colaborador",
                    required = true
            )
            @Valid @RequestBody CollaboratorCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(collaboratorService.create(request));
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation( summary = "Atualizar colaborador", description = "Atualiza os dados de um colaborador existente.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Colaborador atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = CollaboratorDetailResponse.class))),
            @ApiResponse( responseCode = "400", description = "Dados inválidos"),
            @ApiResponse( responseCode = "404", description = "Colaborador não encontrado"),
            @ApiResponse( responseCode = "403", description = "Usuário sem permissão")
    })
    public ResponseEntity<CollaboratorDetailResponse> update(

            @Parameter( description = "ID do colaborador", example = "1" )
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do colaborador",
                    required = true )
            @Valid @RequestBody CollaboratorUpdateRequest request
    ) {
        return ResponseEntity.ok(collaboratorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation( summary = "Desativar colaborador", description = "Realiza a desativação lógica de um colaborador." )
    @ApiResponses(value = {
            @ApiResponse( responseCode = "204", description = "Colaborador desativado com sucesso" ),
            @ApiResponse( responseCode = "404", description = "Colaborador não encontrado"),
            @ApiResponse( responseCode = "403", description = "Usuário sem permissão")
    })
    public ResponseEntity<Void> deactivate(
            @Parameter( description = "ID do colaborador", example = "1")
            @PathVariable Long id ) {
        collaboratorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation( summary = "Reativar colaborador", description = "Reativa um colaborador previamente desativado.")
    @ApiResponses(value = {
            @ApiResponse( responseCode = "204", description = "Colaborador reativado com sucesso"),
            @ApiResponse( responseCode = "404", description = "Colaborador não encontrado" ),
            @ApiResponse( responseCode = "403", description = "Usuário sem permissão")
    })
    public ResponseEntity<Void> reactivate(
            @Parameter( description = "ID do colaborador", example = "1")
            @PathVariable Long id
    ) {
        collaboratorService.reactivate(id);
        return ResponseEntity.noContent().build();
    }
}