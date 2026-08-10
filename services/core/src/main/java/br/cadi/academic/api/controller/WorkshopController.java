package br.cadi.academic.api.controller;

import br.cadi.academic.api.controller.examples.AcademicApiExamples;
import br.cadi.academic.api.dto.request.WorkshopCreateRequest;
import br.cadi.academic.api.dto.response.WorkshopDeactivationPreviewResponse;
import br.cadi.academic.api.dto.response.WorkshopResponse;
import br.cadi.academic.api.dto.request.WorkshopUpdateRequest;
import br.cadi.academic.application.WorkshopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@RequestMapping("/api/v1/workshops")
@Tag(name = "Workshops", description = "Gestão de oficinas: cadastro, consulta e desativação")
public class WorkshopController {

    private final WorkshopService workshopService;

    @GetMapping
    @Operation(
            summary = "Listar oficinas",
            description = """
                    Retorna a lista de oficinas cadastradas, com opção de filtrar apenas as ativas.

                    **GET** `/api/v1/workshops`

                    ## Autenticação
                    - Envie JWT no header `Authorization`: clique em **Authorize** no Swagger e use `Bearer {seu_token}`.
                    - Qualquer usuário autenticado pode listar (demais regras vêm do `SecurityConfig` global).

                    ## Parâmetros de query
                    | Parâmetro | Padrão | Descrição |
                    |-----------|--------|-----------|
                    | activeOnly | true | Se `true`, retorna só oficinas com `active=true` |

                    ## Observações
                    - **401** — sem token ou token inválido/expirado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de oficinas",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.WORKSHOP_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_WORKSHOPS)))
    })
    public ResponseEntity<List<WorkshopResponse>> listAll(
            @Parameter(description = "Filtrar apenas ativas", example = "true")
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(workshopService.listAll(activeOnly));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar oficina por id",
            description = """
                    Consulta uma oficina pelo identificador numérico.

                    **GET** `/api/v1/workshops/{id}`

                    ## Autenticação
                    - JWT obrigatório (`Authorization: Bearer {token}`).

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da oficina |

                    ## Observações
                    - **404** — oficina não existe
                    - **401** — não autenticado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oficina encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.WORKSHOP_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_WORKSHOPS_ID))),
            @ApiResponse(responseCode = "404", description = "Oficina não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_WORKSHOP)))
    })
    public ResponseEntity<WorkshopResponse> getById(
            @Parameter(description = "ID da oficina", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(workshopService.getById(id));
    }

    @GetMapping("/{id}/deactivation-preview")
    @Operation(
            summary = "Pré-visualizar desativação da oficina e turmas em cascata",
            description = """
                    Permite avaliar o impacto antes de desativar a oficina: turmas vinculadas, se há matrículas ativas e se a operação em cascata pode seguir.

                    **GET** `/api/v1/workshops/{id}/deactivation-preview`

                    ## Autenticação
                    - JWT obrigatório (`Bearer {token}`).

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da oficina |

                    ## Corpo da resposta (conceito)
                    | Campo | Descrição |
                    |-------|-----------|
                    | canProceed | `true` se não há bloqueio (ex.: matrículas ativas em turmas ativas) |
                    | schoolRooms | Linhas com dados da turma e flags `active` / `hasActiveEnrollments` |

                    ## Observações
                    - **404** — oficina não encontrada
                    - **401** — não autenticado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Impacto nas turmas vinculadas",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.WORKSHOP_DEACTIVATION_PREVIEW_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_WORKSHOPS_PREVIEW))),
            @ApiResponse(responseCode = "404", description = "Oficina não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_WORKSHOP_PREVIEW)))
    })
    public ResponseEntity<WorkshopDeactivationPreviewResponse> previewDeactivation(
            @Parameter(description = "ID da oficina", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(workshopService.previewDeactivation(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER')")
    @Operation(
            summary = "Criar oficina",
            description = """
                    Cria uma nova oficina ou **reativa** uma inativa com o mesmo nome (após normalização com trim).

                    **POST** `/api/v1/workshops`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papel exigido: **ROLE_ADMIN** (no código: `hasRole('ADMIN')`).

                    ## Corpo (JSON)
                    | Campo | Obrigatório | Descrição |
                    |-------|-------------|-----------|
                    | name | Sim | Nome da oficina; deve ser único entre oficinas **ativas** |

                    ## Observações
                    - **201** — criada ou reativada; corpo com `id`, `name`, `active`
                    - **400** — validação Bean Validation ou JSON malformado
                    - **401** — não autenticado
                    - **403** — autenticado mas sem papel de administrador
                    - **409** — já existe oficina **ativa** com o mesmo nome
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Oficina criada ou reativada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.WORKSHOP_JSON))),
            @ApiResponse(responseCode = "400", description = "JSON inválido ou validação do body",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.VALIDATION_WORKSHOPS))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_WORKSHOPS))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (não é admin)",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_WORKSHOPS))),
            @ApiResponse(responseCode = "409", description = "Nome de oficina já utilizado (ativa)",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_WORKSHOP_NAME)))
    })
    public ResponseEntity<WorkshopResponse> create(@Valid @RequestBody WorkshopCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workshopService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER')")
    @Operation(
            summary = "Atualizar oficina",
            description = """
                    Atualiza o nome da oficina indicada por `id`.

                    **PUT** `/api/v1/workshops/{id}`

                    ## Autenticação
                    - JWT + **ROLE_ADMIN**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da oficina a alterar |

                    ## Corpo (JSON)
                    | Campo | Obrigatório | Descrição |
                    |-------|-------------|-----------|
                    | name | Sim | Novo nome (único entre oficinas ativas, exceto a própria) |

                    ## Observações
                    - **200** — atualizada
                    - **400** — validação
                    - **401** / **403** — autenticação / sem admin
                    - **404** — oficina não existe
                    - **409** — nome já usado por outra oficina ativa
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oficina atualizada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.WORKSHOP_JSON))),
            @ApiResponse(responseCode = "400", description = "Validação do body",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.VALIDATION_WORKSHOPS_ID))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_WORKSHOPS_ID))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (não é admin)",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_WORKSHOPS_ID))),
            @ApiResponse(responseCode = "404", description = "Oficina não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_WORKSHOP))),
            @ApiResponse(responseCode = "409", description = "Nome de oficina já utilizado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_WORKSHOP_NAME_PUT)))
    })
    public ResponseEntity<WorkshopResponse> update(
            @Parameter(description = "ID da oficina", example = "1") @PathVariable Long id,
            @Valid @RequestBody WorkshopUpdateRequest request
    ) {
        return ResponseEntity.ok(workshopService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER')")
    @Operation(
            summary = "Desativar oficina e turmas ativas em cascata",
            description = """
                    Desativa a oficina e, em cascata, todas as **turmas ativas** vinculadas a ela. A operação falha se alguma turma ativa tiver matrículas ativas.

                    **DELETE** `/api/v1/workshops/{id}`

                    ## Autenticação
                    - JWT + **ROLE_ADMIN**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da oficina |

                    ## Resposta de sucesso
                    - **204 No Content** — sem corpo

                    ## Observações
                    - **404** — oficina não encontrada
                    - **409** — mensagem lista turmas que bloqueiam por matrículas ativas
                    - **401** / **403** — autenticação / sem admin
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Oficina e turmas desativadas (sem corpo)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_WORKSHOPS_ID))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (não é admin)",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_WORKSHOPS_ID))),
            @ApiResponse(responseCode = "404", description = "Oficina não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_WORKSHOP))),
            @ApiResponse(responseCode = "409", description = "Turma com matrículas ativas impede a operação",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_WORKSHOP_DEACTIVATE)))
    })
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID da oficina", example = "1") @PathVariable Long id
    ) {
        workshopService.deactivateWithCascade(id);
        return ResponseEntity.noContent().build();
    }
}
