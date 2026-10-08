package br.sing.finance.api.controller;

import br.sing.exception.ApiError;
import br.sing.finance.api.dto.request.FiscalDocumentCreateRequest;
import br.sing.finance.api.dto.request.FiscalDocumentUpdateRequest;
import br.sing.finance.api.dto.response.FiscalDocumentResponse;
import br.sing.finance.application.FiscalDocumentService;
import br.sing.finance.domain.FiscalDocumentType;
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
@RequestMapping("/api/v1/finance/fiscal-documents")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Finanças - Documentos Fiscais", description = "Gestão de documentos fiscais (notas, recibos). Requer papel ADMIN.")
public class FiscalDocumentController {

    private final FiscalDocumentService fiscalDocumentService;

    @GetMapping
    @Operation(
            summary = "Listar documentos fiscais",
            description = "Lista os documentos fiscais, com filtros opcionais por tipo e número do documento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de documentos fiscais retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<FiscalDocumentResponse>> listAll(
            @Parameter(description = "Tipo do documento fiscal", example = "NFE")
            @RequestParam(required = false) FiscalDocumentType type,
            @Parameter(description = "Número do documento fiscal", example = "12345")
            @RequestParam(required = false) String documentNumber
    ) {
        return ResponseEntity.ok(fiscalDocumentService.listAll(type, documentNumber));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar documento fiscal por ID",
            description = "Retorna os dados de um documento fiscal específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento fiscal encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Documento fiscal não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FiscalDocumentResponse> getById(
            @Parameter(description = "ID do documento fiscal", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(fiscalDocumentService.getById(id));
    }

    @PostMapping
    @Operation(
            summary = "Criar documento fiscal",
            description = "Registra um novo documento fiscal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento fiscal criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FiscalDocumentResponse> create(@Valid @RequestBody FiscalDocumentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalDocumentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar documento fiscal",
            description = "Atualiza os dados de um documento fiscal existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento fiscal atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Documento fiscal não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FiscalDocumentResponse> update(
            @Parameter(description = "ID do documento fiscal", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody FiscalDocumentUpdateRequest request
    ) {
        return ResponseEntity.ok(fiscalDocumentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir documento fiscal",
            description = "Remove um documento fiscal existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Documento fiscal excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Documento fiscal não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do documento fiscal", example = "1", required = true)
            @PathVariable Long id) {
        fiscalDocumentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
