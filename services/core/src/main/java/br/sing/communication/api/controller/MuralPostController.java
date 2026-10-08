package br.sing.communication.api.controller;

import br.sing.communication.api.dto.request.MuralPostCreateRequest;
import br.sing.communication.api.dto.request.MuralPostUpdateRequest;
import br.sing.communication.api.dto.response.MuralPostResponse;
import br.sing.communication.application.MuralPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mural")
@RequiredArgsConstructor
@Tag(name = "Mural Institucional", description = "Publicações institucionais com controle de público-alvo e notificações")
public class MuralPostController {

    private final MuralPostService muralPostService;

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    @GetMapping
    @Operation(
            summary = "Listar publicações do mural",
            description = """
                    Retorna as publicações do mural visíveis para o usuário autenticado.

                    ## Visibilidade por perfil
                    | Perfil | Visualiza |
                    |--------|-----------|
                    | ADMIN / SECRETARY | Todos os posts ativos (gestão) |
                    | PROFESSOR / STUDENT / PARENT | Posts para os quais foram destinatários |

                    ## Campo `viewed`
                    Indica se o usuário já visualizou o post. Admin/Secretaria recebem `true`.

                    ## Ordenação
                    Mais recentes primeiro (`createdAt DESC`).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de publicações"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<MuralPostResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(muralPostService.listForCurrentUser(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar publicação por ID",
            description = """
                    Retorna uma publicação específica respeitando as regras de visibilidade.

                    - **403** — publicação existe mas o usuário não é destinatário
                    - **404** — publicação não existe ou foi removida
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicação encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem acesso a esta publicação"),
            @ApiResponse(responseCode = "404", description = "Publicação não encontrada")
    })
    public ResponseEntity<MuralPostResponse> getById(
            @Parameter(description = "ID da publicação", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(muralPostService.getById(id, authentication.getName()));
    }

    // ------------------------------------------------------------------
    // Notificações / badge
    // ------------------------------------------------------------------

    @GetMapping("/unread-count")
    @Operation(
            summary = "Contador de posts não lidos",
            description = """
                    Retorna o número de posts não visualizados pelo usuário autenticado.
                    Usado para exibir o badge de notificação na navbar.

                    **Admin/Secretaria** retornam sempre `0` (não têm rastreamento de leitura).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contador retornado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        long count = muralPostService.getUnreadCount(authentication.getName());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/mark-all-read")
    @Operation(
            summary = "Marcar todos os posts como lidos",
            description = """
                    Marca todos os posts não lidos do usuário autenticado como visualizados.
                    Deve ser chamado quando o usuário entra na página do mural.

                    **Admin/Secretaria** não têm efeito (retorna 204 igualmente).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Todos marcados como lidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        muralPostService.markAllRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARY', 'ADMIN')")
    @Operation(
            summary = "Criar publicação no mural",
            description = """
                    Cria uma nova publicação com público-alvo definido.

                    ## Regras por perfil
                    | Perfil | Campo obrigatório | Público permitido |
                    |--------|-------------------|-------------------|
                    | PROFESSOR | `audienceSchoolroomIds` | Apenas suas próprias turmas |
                    | SECRETARY | `audienceRoles` e/ou `audienceSchoolroomIds` | Qualquer |
                    | ADMIN | `audienceRoles` e/ou `audienceSchoolroomIds` | Qualquer |

                    Ao criar, todos os destinatários recebem um e-mail de notificação (via RabbitMQ).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicação criada"),
            @ApiResponse(responseCode = "400", description = "Validação do body"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão ou audiência inválida para o perfil")
    })
    public ResponseEntity<MuralPostResponse> create(
            @Valid @RequestBody MuralPostCreateRequest request,
            Authentication authentication
    ) {
        MuralPostResponse response = muralPostService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARY', 'ADMIN')")
    @Operation(
            summary = "Atualizar publicação",
            description = """
                    Atualiza título, conteúdo e público-alvo de uma publicação.

                    - **PROFESSOR** — somente seus próprios posts (audiência restrita às suas turmas).
                    - **SECRETARY / ADMIN** — qualquer post.

                    Novos destinatários recebem notificação por e-mail.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicação atualizada"),
            @ApiResponse(responseCode = "400", description = "Validação do body"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar"),
            @ApiResponse(responseCode = "404", description = "Publicação não encontrada")
    })
    public ResponseEntity<MuralPostResponse> update(
            @Parameter(description = "ID da publicação", example = "1") @PathVariable Long id,
            @Valid @RequestBody MuralPostUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(muralPostService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARY', 'ADMIN')")
    @Operation(
            summary = "Remover publicação (soft-delete)",
            description = """
                    Desativa uma publicação do mural (soft-delete).

                    - **PROFESSOR** — somente seus próprios posts.
                    - **SECRETARY / ADMIN** — qualquer post.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Publicação removida (sem corpo)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para remover"),
            @ApiResponse(responseCode = "404", description = "Publicação não encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da publicação", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        muralPostService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
