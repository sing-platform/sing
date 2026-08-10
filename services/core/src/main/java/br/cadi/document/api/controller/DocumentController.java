package br.cadi.document.api.controller;

import br.cadi.document.api.controller.examples.DocumentApiExamples;
import br.cadi.document.api.dto.request.AtestadoApproveRequest;
import br.cadi.document.api.dto.request.AtestadoRejectRequest;
import br.cadi.document.api.dto.request.DocumentUploadRequest;
import br.cadi.document.api.dto.request.DocumentUploadSwaggerBody;
import br.cadi.document.api.dto.response.DocumentResponse;
import br.cadi.document.application.AtestadoApprovalService;
import br.cadi.document.application.DocumentService;
import br.cadi.document.application.DocumentService.DocumentDownload;
import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.document.domain.enums.DocumentScope;
import br.cadi.iam.domain.User;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Gestão documental de usuários do CADI")
public class DocumentController {

    private static final Set<String> ADMIN_ROLES =
            Set.of("ROLE_SECRETARY", "ROLE_ADMIN", "ROLE_MANAGER");

    private final DocumentService documentService;
    private final AtestadoApprovalService atestadoApprovalService;

    // ── helper: evita duplicação da lógica de role em cada endpoint ───────────

    /**
     * Retorna {@code true} se o usuário possui perfil de secretaria ou administração.
     * Centralizado para garantir que qualquer mudança nos roles seja aplicada
     * em um único lugar.
     */
    private boolean isAdminOrSecretary(User user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_ROLES::contains);
    }

    // --- Aluno / Responsável / Funcionário: upload dos próprios documentos ---

    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_STUDENT','ROLE_PARENT','ROLE_PROFESSOR','ROLE_STAFF','ROLE_VOLUNTEER')")
    @Operation(
            summary = "Upload do próprio documento",
            description = """
                    Faz upload de um documento pessoal do usuário autenticado.

                    **POST** `/api/documents/me`

                    ## Autenticação
                    - JWT obrigatório (`Authorization: Bearer {token}`).
                    - Papéis permitidos: **ROLE_STUDENT**, **ROLE_PARENT**, **ROLE_PROFESSOR**, **ROLE_STAFF**, **ROLE_VOLUNTEER**.

                    ## Campos do formulário
                    | Campo | Tipo | Descrição |
                    |-------|------|-----------|
                    | file | binary | Arquivo (PDF, JPEG, PNG, WEBP — máx. 10 MB) |
                    | category | enum | Categoria do documento |
                    | description | string | Descrição opcional (máx. 500 chars) |

                    ## Categorias permitidas para auto-upload
                    `ATESTADO_MEDICO`, `JUSTIFICATIVA_FALTA`

                    ## Observações
                    - **201** — documento criado
                    - **400** — tipo de arquivo inválido ou categoria não permitida
                    - **401** / **403** — autenticação / sem permissão
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento enviado com sucesso",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.DOCUMENT_JSON))),
            @ApiResponse(responseCode = "400", description = "Arquivo ou dados inválidos",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN)))
    })
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = DocumentUploadSwaggerBody.class)))
    public ResponseEntity<DocumentResponse> uploadOwn(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @RequestParam DocumentCategory category,
            @Parameter(hidden = true) @RequestParam(required = false) String description,
            @Parameter(hidden = true) @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDateStart,
            @Parameter(hidden = true) @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDateEnd) {

        Long personId = documentService.resolvePersonIdFromUserId(user.getId());
        DocumentResponse response = documentService.uploadOwn(
                personId, user.getId(), file,
                new DocumentUploadRequest(category, description, referenceDateStart, referenceDateEnd));
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping(value = "/student/{studentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_PARENT')")
    @Operation(
            summary = "Responsável envia documento do aluno",
            description = """
                    Responsável autenticado envia atestado ou justificativa em nome de um aluno vinculado.

                    **POST** `/api/documents/student/{studentId}`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papel exigido: **ROLE_PARENT**.
                    - O vínculo guardian → aluno é validado no backend.
                    """
    )
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = DocumentUploadSwaggerBody.class)))
    public ResponseEntity<DocumentResponse> uploadForStudentAsGuardian(
            @Parameter(description = "ID do aluno", example = "42") @PathVariable Long studentId,
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @RequestParam DocumentCategory category,
            @Parameter(hidden = true) @RequestParam(required = false) String description,
            @Parameter(hidden = true) @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDateStart,
            @Parameter(hidden = true) @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDateEnd) {

        DocumentResponse response = documentService.uploadForStudentAsGuardian(
                studentId, user.getId(), file,
                new DocumentUploadRequest(category, description, referenceDateStart, referenceDateEnd));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ROLE_PARENT')")
    @Operation(
            summary = "Responsável lista documentos do aluno",
            description = """
                    Retorna documentos de um aluno vinculado ao responsável autenticado.

                    **GET** `/api/documents/student/{studentId}`
                    """
    )
    public ResponseEntity<List<DocumentResponse>> listForStudentAsGuardian(
            @Parameter(description = "ID do aluno", example = "42") @PathVariable Long studentId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                documentService.listForStudentAsGuardian(studentId, user.getId()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ROLE_STUDENT','ROLE_PARENT','ROLE_PROFESSOR','ROLE_STAFF','ROLE_VOLUNTEER')")
    @Operation(
            summary = "Listar próprios documentos",
            description = """
                    Retorna todos os documentos ativos do usuário autenticado.

                    **GET** `/api/documents/me`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis permitidos: **ROLE_STUDENT**, **ROLE_PARENT**, **ROLE_PROFESSOR**, **ROLE_STAFF**, **ROLE_VOLUNTEER**.

                    ## Observações
                    - Documentos com `scope = THERAPEUTIC_PRIVATE` nunca aparecem nesta listagem.
                    - **401** / **403** — autenticação / sem permissão
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de documentos do usuário",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.DOCUMENT_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN)))
    })
    public ResponseEntity<List<DocumentResponse>> listOwn(@AuthenticationPrincipal User user) {
        Long personId = documentService.resolvePersonIdFromUserId(user.getId());
        return ResponseEntity.ok(documentService.listOwn(personId));
    }

    // --- Secretaria / Admin: CRUD de documentos de qualquer pessoa ---

    @PostMapping(value = "/person/{personId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(
            summary = "Upload de documento para qualquer pessoa",
            description = """
                    Secretaria ou administrador faz upload de documento vinculado a uma pessoa específica.

                    **POST** `/api/documents/person/{personId}`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis permitidos: **ROLE_SECRETARY**, **ROLE_ADMIN**, **ROLE_MANAGER**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | personId | ID da pessoa dona do documento |

                    ## Campos do formulário
                    | Campo | Tipo | Descrição |
                    |-------|------|-----------|
                    | file | binary | Arquivo (PDF, JPEG, PNG, WEBP — máx. 10 MB) |
                    | category | enum | Categoria do documento |
                    | description | string | Descrição opcional (máx. 500 chars) |

                    ## Observações
                    - O documento é criado com `scope = STUDENT_PERSONAL`.
                    - **404** — pessoa não encontrada
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento criado com sucesso",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.DOCUMENT_JSON))),
            @ApiResponse(responseCode = "400", description = "Arquivo ou dados inválidos",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.NOT_FOUND)))
    })
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = DocumentUploadSwaggerBody.class)))
    public ResponseEntity<DocumentResponse> uploadForPerson(
            @Parameter(description = "ID da pessoa", example = "42") @PathVariable Long personId,
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @RequestParam DocumentCategory category,
            @Parameter(hidden = true) @RequestParam(required = false) String description,
            @Parameter(hidden = true) @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDateStart,
            @Parameter(hidden = true) @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDateEnd) {

        DocumentResponse response = documentService.uploadForPerson(
                personId, user.getId(), file,
                new DocumentUploadRequest(category, description, referenceDateStart, referenceDateEnd),
                DocumentScope.STUDENT_PERSONAL);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/person/{personId}")
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(
            summary = "Listar documentos de uma pessoa",
            description = """
                    Retorna todos os documentos ativos de uma pessoa específica.

                    **GET** `/api/documents/person/{personId}`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis permitidos: **ROLE_SECRETARY**, **ROLE_ADMIN**, **ROLE_MANAGER**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | personId | ID da pessoa |

                    ## Observações
                    - Documentos com `scope = THERAPEUTIC_PRIVATE` **não** aparecem nesta listagem.
                    - **404** — pessoa não encontrada
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de documentos da pessoa",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.DOCUMENT_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.NOT_FOUND)))
    })
    public ResponseEntity<List<DocumentResponse>> listByPerson(
            @Parameter(description = "ID da pessoa", example = "42") @PathVariable Long personId) {
        return ResponseEntity.ok(documentService.listByPerson(personId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(
            summary = "Atualizar descrição do documento",
            description = """
                    Atualiza a descrição de um documento existente.

                    **PUT** `/api/documents/{id}`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis permitidos: **ROLE_SECRETARY**, **ROLE_ADMIN**, **ROLE_MANAGER**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID do documento |

                    ## Parâmetros de query
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | description | Nova descrição (máx. 500 caracteres) |

                    ## Observações
                    - **404** — documento não encontrado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento atualizado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.DOCUMENT_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.NOT_FOUND)))
    })
    public ResponseEntity<DocumentResponse> updateDescription(
            @Parameter(description = "ID do documento", example = "1") @PathVariable Long id,
            @Parameter(description = "Nova descrição", example = "Atestado revisado") @RequestParam String description) {
        return ResponseEntity.ok(documentService.updateDescription(id, description));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER','ROLE_PSYCHOLOGIST')")
    @Operation(
            summary = "Excluir documento (soft delete + remoção do blob)",
            description = """
                    Marca o documento como inativo (soft delete) **e remove o arquivo do Azure Blob Storage**.

                    **DELETE** `/api/documents/{id}`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis permitidos: **ROLE_SECRETARY**, **ROLE_ADMIN**, **ROLE_MANAGER**, **ROLE_PSYCHOLOGIST**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID do documento |

                    ## Regras de acesso
                    | Papel | Pode excluir |
                    |-------|-------------|
                    | SECRETARY / ADMIN / MANAGER | Qualquer documento (exceto THERAPEUTIC_PRIVATE de outro psicólogo) |
                    | PSYCHOLOGIST | Somente documentos que ele próprio enviou |

                    ## Observações
                    - **204** — excluído com sucesso, sem corpo
                    - **403** — psicólogo tentando excluir documento de outro psicólogo
                    - **404** — documento não encontrado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Documento excluído (sem corpo)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para excluir este documento",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.NOT_FOUND)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do documento", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        documentService.delete(id, user.getId(), isAdminOrSecretary(user));
        return ResponseEntity.noContent().build();
    }

    // --- Psicólogo: anotações terapêuticas privadas ---

    @PostMapping(value = "/patient/{patientPersonId}/therapeutic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_PSYCHOLOGIST')")
    @Operation(
            summary = "Upload de anotação terapêutica privada",
            description = """
                    Psicólogo faz upload de anotação terapêutica vinculada a um paciente.

                    **POST** `/api/documents/patient/{patientPersonId}/therapeutic`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papel exigido: **ROLE_PSYCHOLOGIST**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | patientPersonId | ID da pessoa (paciente) |

                    ## Campos do formulário
                    | Campo | Tipo | Descrição |
                    |-------|------|-----------|
                    | file | binary | Arquivo (PDF, JPEG, PNG, WEBP — máx. 10 MB) |
                    | category | enum | Categoria do documento |
                    | description | string | Descrição opcional (máx. 500 chars) |

                    ## Regra de privacidade
                    O documento é criado com `scope = THERAPEUTIC_PRIVATE`. **Somente o psicólogo que fez o upload pode visualizá-lo ou excluí-lo.**

                    ## Observações
                    - **404** — paciente não encontrado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Anotação terapêutica enviada",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.THERAPEUTIC_JSON))),
            @ApiResponse(responseCode = "400", description = "Arquivo ou dados inválidos",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (não é psicólogo)",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.NOT_FOUND)))
    })
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = DocumentUploadSwaggerBody.class)))
    public ResponseEntity<DocumentResponse> uploadTherapeutic(
            @Parameter(description = "ID da pessoa (paciente)", example = "55") @PathVariable Long patientPersonId,
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @RequestParam DocumentCategory category,
            @Parameter(hidden = true) @RequestParam(required = false) String description) {

        DocumentResponse response = documentService.uploadTherapeutic(
                patientPersonId, user.getId(), file,
                new DocumentUploadRequest(category, description));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/patient/{patientPersonId}/therapeutic")
    @PreAuthorize("hasRole('ROLE_PSYCHOLOGIST')")
    @Operation(
            summary = "Listar anotações terapêuticas do paciente",
            description = """
                    Retorna as anotações terapêuticas do psicólogo autenticado para um paciente específico.

                    **GET** `/api/documents/patient/{patientPersonId}/therapeutic`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papel exigido: **ROLE_PSYCHOLOGIST**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | patientPersonId | ID da pessoa (paciente) |

                    ## Regra de privacidade
                    Retorna **somente** os documentos que **este psicólogo** enviou. Anotações de outros psicólogos para o mesmo paciente não são visíveis.

                    ## Observações
                    - **404** — paciente não encontrado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de anotações terapêuticas",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.THERAPEUTIC_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão (não é psicólogo)",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.NOT_FOUND)))
    })
    public ResponseEntity<List<DocumentResponse>> listTherapeutic(
            @Parameter(description = "ID da pessoa (paciente)", example = "55") @PathVariable Long patientPersonId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(documentService.listTherapeutic(patientPersonId, user.getId()));
    }

    // --- Atestados: fluxo de aprovacao pela secretaria ---

    @GetMapping("/atestados/pending")
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(
            summary = "Listar atestados pendentes",
            description = """
                    Retorna atestados medicos e justificativas de falta com status PENDING,
                    ordenados pelo mais antigo (FIFO).

                    **GET** `/api/documents/atestados/pending`
                    """
    )
    public ResponseEntity<List<DocumentResponse>> listPendingAtestados() {
        return ResponseEntity.ok(atestadoApprovalService.listPending());
    }

    @GetMapping("/atestados")
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(
            summary = "Listar todos os atestados",
            description = "Retorna atestados e justificativas em qualquer status (PENDING/APPROVED/REJECTED)."
    )
    public ResponseEntity<List<DocumentResponse>> listAllAtestados() {
        return ResponseEntity.ok(atestadoApprovalService.listAll());
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(
            summary = "Aprovar atestado",
            description = """
                    Aprova o atestado para o periodo informado. Justifica automaticamente
                    todas as faltas (status ABSENT) do aluno cujas datas caem dentro do intervalo,
                    em todas as oficinas. Operacao idempotente: reaprovar com novas datas reverte
                    a justificativa anterior antes de aplicar o novo periodo.
                    """
    )
    public ResponseEntity<DocumentResponse> approveAtestado(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            // FQN aqui: o import `io.swagger.v3.oas.annotations.parameters.RequestBody`
            // (usado nas anotacoes de documentacao do upload) sombreia
            // org.springframework.web.bind.annotation.RequestBody — sem o FQN
            // o Spring nao desserializa o body e os campos chegam null.
            @Valid @org.springframework.web.bind.annotation.RequestBody AtestadoApproveRequest request) {

        return ResponseEntity.ok(
                atestadoApprovalService.approve(id, user.getId(), request.startDate(), request.endDate())
        );
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ROLE_SECRETARY','ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(
            summary = "Rejeitar atestado",
            description = "Rejeita o atestado. Motivo obrigatorio (max. 500 caracteres)."
    )
    public ResponseEntity<DocumentResponse> rejectAtestado(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @org.springframework.web.bind.annotation.RequestBody AtestadoRejectRequest request) {

        return ResponseEntity.ok(
                atestadoApprovalService.reject(id, user.getId(), request.reason())
        );
    }

    // --- Download (todos os perfis autenticados; service valida ownership) ---

    @GetMapping("/{id}/download")
    @Operation(
            summary = "Download de documento",
            description = """
                    Faz o download do arquivo de um documento.

                    **GET** `/api/documents/{id}/download`

                    ## Autenticação
                    - JWT obrigatório. Qualquer perfil autenticado pode tentar; o service valida o acesso.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID do documento |

                    ## Regras de acesso
                    | Tipo de documento | Quem pode baixar |
                    |-------------------|------------------|
                    | STUDENT_PERSONAL / INSTITUTIONAL | Dono (por person.id), SECRETARY, ADMIN, MANAGER |
                    | THERAPEUTIC_PRIVATE | Somente o psicólogo que enviou (por user.id) |

                    ## Observações
                    - Resposta com `Content-Disposition: attachment; filename=...` (UTF-8 seguro).
                    - **403** — documento THERAPEUTIC_PRIVATE acessado por outro usuário
                    - **404** — documento não encontrado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo retornado como stream binário"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para baixar este documento",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado",
                    content = @Content(examples = @ExampleObject(value = DocumentApiExamples.NOT_FOUND)))
    })
    public ResponseEntity<InputStreamResource> download(
            @Parameter(description = "ID do documento", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        DocumentDownload download = documentService.download(id, user.getId(), isAdminOrSecretary(user));

        // Content-Disposition com suporte a UTF-8 (RFC 5987) e sanitização contra header injection
        String safeName = download.originalFileName().replaceAll("[\\r\\n\"\\\\]", "_");
        String encoded = URLEncoder.encode(download.originalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + safeName
                + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(download.stream()));
    }
}
