package br.sing.audience.api;

import br.sing.audience.application.MuralAudienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/mural")
@Tag(name = "Internal", description = "Endpoints consumidos por outros servicos")
public class InternalMuralAudienceController {

    private final MuralAudienceService audienceService;

    @PostMapping("/destinatarios")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARY', 'ADMIN')")
    @Operation(
            summary = "Resolve autor e destinatários de uma publicação do mural",
            description = "Aplica as regras de público-alvo do autor autenticado e devolve os destinatários habilitados, sem o autor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor e destinatários"),
            @ApiResponse(responseCode = "400", description = "Público-alvo não informado"),
            @ApiResponse(responseCode = "403", description = "Público-alvo não permitido ao autor")
    })
    public ResponseEntity<AudienceResponse> resolve(
            @RequestBody AudienceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(audienceService.resolve(authentication.getName(), request));
    }
}
