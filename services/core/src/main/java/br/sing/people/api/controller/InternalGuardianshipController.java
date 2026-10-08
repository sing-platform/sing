package br.sing.people.api.controller;

import br.sing.people.application.StudentGuardianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API interna consumida por outros servicos — nao pelo frontend.
 *
 * <p>Existe porque o vinculo responsavel-aluno vive no dominio {@code people},
 * mas o servico de documentos precisa dele para autorizar upload/listagem de
 * atestado feito por um responsavel. Antes da extracao isso era uma chamada
 * direta a {@code StudentGuardianService}.</p>
 *
 * <p>E um contrato proprio e minimo (responde apenas um booleano) em vez de
 * reaproveitar o DTO de tela: assim a fronteira entre os servicos nao fica
 * amarrada a mudancas de apresentacao.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/guardianship")
@Tag(name = "Internal", description = "Endpoints consumidos por outros servicos")
public class InternalGuardianshipController {

    private final StudentGuardianService studentGuardianService;

    @GetMapping("/{studentId}/{guardianPersonId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Verifica se a pessoa e responsavel pelo aluno")
    public ResponseEntity<Map<String, Boolean>> isGuardianOf(
            @PathVariable Long studentId,
            @PathVariable Long guardianPersonId
    ) {
        boolean result = studentGuardianService.isGuardianOf(guardianPersonId, studentId);
        return ResponseEntity.ok(Map.of("isGuardian", result));
    }
}
