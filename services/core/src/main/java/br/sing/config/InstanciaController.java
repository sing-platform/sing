package br.sing.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identidade mínima da réplica que atendeu. Sem JWT: só o nome, nada de env ou IP.
 */
@RestController
@Tag(name = "Ops")
public class InstanciaController {

    public record InstanciaResponse(String servico, String instancia) {
    }

    @GetMapping("/api/v1/instancia")
    @SecurityRequirements
    @Operation(summary = "Nome real da instância que respondeu (pod no cluster, hostname no compose)")
    public ResponseEntity<InstanciaResponse> instancia() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new InstanciaResponse("core", PodHeaderFilter.nomeInstancia()));
    }
}
