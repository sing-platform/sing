package br.sing.reports.api.controller;

import br.sing.reports.api.dto.ReportMetadataResponse;
import br.sing.reports.api.dto.ReportResultResponse;
import br.sing.reports.application.ReportExportService;
import br.sing.reports.application.ReportService;
import br.sing.reports.domain.ReportResult;
import br.sing.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER')")
@Tag(name = "Relatórios", description = "Consulta e exportação de relatórios institucionais")
public class ReportsController {

    private final ReportService reportService;
    private final ReportExportService exportService;

    @GetMapping
    @Operation(summary = "Listar relatórios disponíveis",
            description = "Retorna os metadados de todos os relatórios institucionais disponíveis para consulta/exportação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de relatórios retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN, SECRETARY ou MANAGER)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<ReportMetadataResponse>> list() {
        return ResponseEntity.ok(reportService.listMetadata());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Executar relatório (JSON)",
            description = "Executa o relatório identificado por `slug`, aplicando os parâmetros informados via query string, e retorna o resultado em JSON.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório executado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros do relatório inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN, SECRETARY ou MANAGER)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Relatório não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReportResultResponse> run(
            @Parameter(description = "Identificador do relatório (slug)", example = "frequencia-mensal", required = true)
            @PathVariable String slug,
            @Parameter(description = "Parâmetros do relatório (pares chave/valor via query string)")
            @RequestParam Map<String, String> params
    ) {
        return ResponseEntity.ok(reportService.run(slug, params));
    }

    @GetMapping("/{slug}/export")
    @Operation(summary = "Exportar relatório (CSV ou XLSX)",
            description = "Executa o relatório e retorna o arquivo exportado no formato indicado (`csv` ou `xlsx`).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo exportado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros do relatório inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN, SECRETARY ou MANAGER)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Relatório não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<byte[]> export(
            @Parameter(description = "Identificador do relatório (slug)", example = "frequencia-mensal", required = true)
            @PathVariable String slug,
            @Parameter(description = "Formato de exportação", example = "csv")
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = "Parâmetros do relatório (pares chave/valor via query string)")
            @RequestParam Map<String, String> params
    ) {
        ReportExportService.ExportFormat exportFormat = parseFormat(format);
        ReportResult result = reportService.runForExport(slug, params);
        byte[] body = exportService.export(result, exportFormat);
        String filename = slug + "." + exportService.fileExtension(exportFormat);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportService.contentType(exportFormat)))
                .body(body);
    }

    private static ReportExportService.ExportFormat parseFormat(String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            return ReportExportService.ExportFormat.XLSX;
        }
        return ReportExportService.ExportFormat.CSV;
    }
}
