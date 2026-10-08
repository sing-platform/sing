package br.sing.academic.api.controller;

import br.sing.academic.application.DeclarationService;
import br.sing.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

 /**
 * Endpoint para emissão da Declaração de Matrícula em PDF.
 */
@RestController
@RequestMapping("/api/v1/declaracoes")
@RequiredArgsConstructor
@Tag(name = "Declaration", description = "Generate student enrollment and attendance declaration")
public class DeclarationController {

    private final DeclarationService declaracaoService;

    private static final DateTimeFormatter DATE_FILE =
            DateTimeFormatter.ofPattern("yyyyMMdd");


    @GetMapping("/matricula")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Emite a declaração de matrícula de um aluno em uma turma.",
            description = "Gera e retorna, em PDF, a declaração de matrícula e frequência do aluno na turma informada. "
                    + "Use `inline=false` para forçar download como anexo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Aluno ou matrícula não encontrados",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<byte[]> emitirDeclaracao(
            @Parameter(description = "ID do aluno", example = "1", required = true)
            @RequestParam Long studentId,
            @Parameter(description = "ID da turma", example = "1", required = true)
            @RequestParam Long schoolRoomId,
            @Parameter(description = "Exibir no navegador (true) ou baixar como anexo (false)", example = "true")
            @RequestParam(defaultValue = "true") boolean inline
    ) throws IOException {

        byte[] pdf = declaracaoService.gerarDeclaracao(studentId, schoolRoomId);

        String filename = String.format(
                "declaracao_matricula_%d_%s.pdf",
                studentId,
                LocalDate.now().format(DATE_FILE));

        ContentDisposition disposition = inline
                ? ContentDisposition.inline().filename(filename).build()
                : ContentDisposition.attachment().filename(filename).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(disposition);
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}