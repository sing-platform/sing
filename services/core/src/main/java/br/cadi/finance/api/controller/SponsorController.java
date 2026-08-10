package br.cadi.finance.api.controller;

import br.cadi.exception.ApiError;
import br.cadi.finance.api.dto.request.SponsorCreateRequest;
import br.cadi.finance.api.dto.request.SponsorUpdateRequest;
import br.cadi.finance.api.dto.response.FinancialEntryResponse;
import br.cadi.finance.api.dto.response.SponsorResponse;
import br.cadi.finance.application.FinancialEntryService;
import br.cadi.finance.application.SponsorService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/sponsors")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Finanças - Patrocinadores", description = "Gestão de patrocinadores e seus lançamentos financeiros. Requer papel ADMIN.")
public class SponsorController {

    private final SponsorService sponsorService;
    private final FinancialEntryService financialEntryService;

    @GetMapping
    @Operation(
            summary = "Listar patrocinadores",
            description = "Retorna todos os patrocinadores cadastrados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de patrocinadores retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<SponsorResponse>> listAll() {
        return ResponseEntity.ok(sponsorService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar patrocinador por ID",
            description = "Retorna os dados de um patrocinador específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patrocinador encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patrocinador não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SponsorResponse> getById(
            @Parameter(description = "ID do patrocinador", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(sponsorService.getById(id));
    }

    @GetMapping("/{id}/entries")
    @Operation(
            summary = "Listar lançamentos de um patrocinador",
            description = "Retorna os lançamentos financeiros vinculados ao patrocinador informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de lançamentos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patrocinador não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<FinancialEntryResponse>> listEntries(
            @Parameter(description = "ID do patrocinador", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(financialEntryService.listBySponsor(id));
    }

    @PostMapping
    @Operation(
            summary = "Criar patrocinador",
            description = "Cadastra um novo patrocinador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Patrocinador criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SponsorResponse> create(@Valid @RequestBody SponsorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sponsorService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar patrocinador",
            description = "Atualiza os dados de um patrocinador existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patrocinador atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patrocinador não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SponsorResponse> update(
            @Parameter(description = "ID do patrocinador", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SponsorUpdateRequest request) {
        return ResponseEntity.ok(sponsorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir patrocinador",
            description = "Remove um patrocinador existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Patrocinador excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patrocinador não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do patrocinador", example = "1", required = true)
            @PathVariable Long id) {
        sponsorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
