package br.cadi.finance.api.controller;

import br.cadi.exception.ApiError;
import br.cadi.finance.api.dto.request.FinancialEntryCreateRequest;
import br.cadi.finance.api.dto.request.FinancialEntryUpdateRequest;
import br.cadi.finance.api.dto.request.MarkEntryAsPaidRequest;
import br.cadi.finance.api.dto.response.FinancialEntryResponse;
import br.cadi.finance.api.dto.response.FinancialSummaryResponse;
import br.cadi.finance.application.FinancialEntryService;
import br.cadi.finance.domain.EntryDirection;
import br.cadi.finance.domain.EntryStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/entries")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Finanças - Lançamentos", description = "Gestão de lançamentos financeiros (receitas e despesas). Requer papel ADMIN.")
public class FinancialEntryController {

    private final FinancialEntryService financialEntryService;

    @GetMapping
    @Operation(
            summary = "Listar lançamentos financeiros",
            description = "Lista os lançamentos financeiros, com filtros opcionais por mês, direção (receita/despesa), status e categoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de lançamentos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<FinancialEntryResponse>> list(
            @Parameter(description = "Mês de competência (yyyy-MM)", example = "2026-01")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(description = "Direção do lançamento", example = "INCOME")
            @RequestParam(required = false) EntryDirection direction,
            @Parameter(description = "Status do lançamento", example = "PAID")
            @RequestParam(required = false) EntryStatus status,
            @Parameter(description = "Categoria do lançamento", example = "Mensalidade")
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(financialEntryService.list(month, direction, status, category));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar lançamento financeiro por ID",
            description = "Retorna os dados de um lançamento financeiro específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamento encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FinancialEntryResponse> getById(
            @Parameter(description = "ID do lançamento financeiro", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(financialEntryService.getById(id));
    }

    @PostMapping
    @Operation(
            summary = "Criar lançamento financeiro",
            description = "Registra um novo lançamento financeiro (receita ou despesa)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lançamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FinancialEntryResponse> create(@Valid @RequestBody FinancialEntryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financialEntryService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar lançamento financeiro",
            description = "Atualiza os dados de um lançamento financeiro existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamento atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FinancialEntryResponse> update(
            @Parameter(description = "ID do lançamento financeiro", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody FinancialEntryUpdateRequest request
    ) {
        return ResponseEntity.ok(financialEntryService.update(id, request));
    }

    @PatchMapping("/{id}/pay")
    @Operation(
            summary = "Marcar lançamento como pago",
            description = "Registra o pagamento/recebimento de um lançamento, informando a data de quitação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamento marcado como pago"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FinancialEntryResponse> markAsPaid(
            @Parameter(description = "ID do lançamento financeiro", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody MarkEntryAsPaidRequest request
    ) {
        return ResponseEntity.ok(financialEntryService.markAsPaid(id, request.paidAt()));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Resumo financeiro do mês",
            description = "Retorna o resumo financeiro consolidado (totais de receitas, despesas e saldo) para o mês informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Mês inválido ou não informado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FinancialSummaryResponse> summary(
            @Parameter(description = "Mês de competência (yyyy-MM)", example = "2026-01", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ResponseEntity.ok(financialEntryService.monthSummary(month));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir lançamento financeiro",
            description = "Remove um lançamento financeiro existente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lançamento excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do lançamento financeiro", example = "1", required = true)
            @PathVariable Long id) {
        financialEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
