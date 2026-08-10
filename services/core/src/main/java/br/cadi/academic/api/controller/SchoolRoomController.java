package br.cadi.academic.api.controller;

import br.cadi.academic.api.controller.examples.AcademicApiExamples;
import br.cadi.academic.api.dto.request.SchoolRoomCreateRequest;
import br.cadi.academic.api.dto.request.SchoolRoomUpdateRequest;
import br.cadi.academic.api.dto.response.AdminScheduleBoardResponse;
import br.cadi.academic.api.dto.response.EnrollmentLinkResponse;
import br.cadi.academic.api.dto.response.SchoolRoomResponse;
import br.cadi.academic.api.dto.response.SchoolRoomStudentResponse;
import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.academic.application.SchoolRoomService;
import br.cadi.common.application.AuditLogService;
import br.cadi.people.api.dto.request.AssignTeacherRequest;
import br.cadi.people.api.dto.response.TeacherAssignmentLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schoolrooms")
@Tag(name = "SchoolRooms", description = "Gestão de turmas: cadastro, matrículas, alunos, grade e professores")
public class SchoolRoomController {

    private final SchoolRoomService schoolRoomService;
    private final EnrollmentQueryService enrollmentQueryService;
    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER', 'PROFESSOR', 'PSYCHOLOGIST', 'STAFF', 'VOLUNTEER')")
    @Operation(
            summary = "Listar turmas",
            description = """
                    Lista turmas do sistema com filtro opcional por status ativo.

                    **GET** `/api/v1/schoolrooms`

                    ## Autenticação
                    - JWT obrigatório: **Authorize** → `Bearer {seu_token}`.

                    ## Parâmetros de query
                    | Parâmetro | Padrão | Descrição |
                    |-----------|--------|-----------|
                    | activeOnly | true | Se `true`, retorna apenas turmas com `active=true` |
                    | (corpo) | — | Cada turma inclui `activeEnrollmentCount` e `vacancies` (null se sem capacidade) |

                    ## Observações
                    - **401** — não autenticado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de turmas",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.SCHOOLROOM_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS)))
    })
    public ResponseEntity<List<SchoolRoomResponse>> listAll(
            @Parameter(description = "Filtrar apenas turmas ativas", example = "true")
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(schoolRoomService.listAll(activeOnly));
    }

    @GetMapping("/schedule-board")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'MANAGER')")
    @Operation(summary = "Dados agregados para grade horária (turmas + alunos por turma)")
    @ApiResponse(responseCode = "200", description = "Turmas ativas e mapa de alunos matriculados por turma")
    public ResponseEntity<AdminScheduleBoardResponse> scheduleBoard() {
        return ResponseEntity.ok(schoolRoomService.getAdminScheduleBoardData());
    }

    @GetMapping("/{schoolRoomId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'PROFESSOR')")
    @Operation(
            summary = "Listar vínculos aluno-turma (matrículas) na turma",
            description = """
                    Retorna a página de matrículas (vínculo aluno–turma) para a turma informada, com paginação Spring Data.

                    **GET** `/api/v1/schoolrooms/{schoolRoomId}/enrollments`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis: **ADMIN**, **SECRETARY** ou **PROFESSOR** (o backend restringe o escopo: professor só acessa turmas onde pode atuar).

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | schoolRoomId | ID da turma |

                    ## Parâmetros de query
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | active | Opcional; filtra matrículas ativas (`true`) ou inativas (`false`) |
                    | page, size, sort | Paginação Spring (`page` base 0); padrão: `size=20`, `sort=enrollmentDate,desc` |

                    ## Observações
                    - **200** — objeto página (`content`, `totalElements`, etc.)
                    - **404** — turma não existe
                    - **403** — sem permissão para esta turma
                    - **401** — não autenticado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de matrículas (`content`, `totalElements`, …)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para esta turma",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_SCHOOLROOM_ENROLLMENTS))),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_SCHOOLROOM)))
    })
    public ResponseEntity<Page<EnrollmentLinkResponse>> pageEnrollmentsBySchoolRoom(
            @Parameter(description = "ID da turma", example = "10") @PathVariable Long schoolRoomId,
            @Parameter(description = "Filtrar por matrícula ativa (omitir = todas)")
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "enrollmentDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                enrollmentQueryService.pageBySchoolRoom(schoolRoomId, active, pageable, authentication)
        );
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'PROFESSOR')")
    @Operation(
            summary = "Listar alunos matriculados ativos na turma",
            description = """
                    Lista alunos com matrícula **ativa** na turma (dados resumidos para uso pedagógico/operacional).

                    **GET** `/api/v1/schoolrooms/{id}/students`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis: **ADMIN**, **SECRETARY** ou **PROFESSOR** (com checagem de escopo no serviço).

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da turma |

                    ## Observações
                    - **200** — lista de `SchoolRoomStudentResponse`
                    - **404** — turma não encontrada
                    - **403** — sem permissão para esta turma
                    - **401** — não autenticado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de alunos (corpo definido por `SchoolRoomStudentResponse`)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para esta turma",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_SCHOOLROOM_STUDENTS))),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_SCHOOLROOM)))
    })
    public ResponseEntity<List<SchoolRoomStudentResponse>> listStudents(
            @Parameter(description = "ID da turma", example = "10") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(schoolRoomService.listStudents(id, authentication));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar turma por id",
            description = """
                    Retorna os dados completos da turma: nome, grade (`scheduleSlots`), sala, capacidade, oficina, professor titular, etc.

                    **GET** `/api/v1/schoolrooms/{id}`

                    ## Autenticação
                    - JWT obrigatório (`Bearer {token}`).

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da turma |

                    ## Observações
                    - **404** — turma não encontrada
                    - **401** — não autenticado
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turma encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.SCHOOL_ROOM_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_SCHOOLROOM)))
    })
    public ResponseEntity<SchoolRoomResponse> getById(
            @Parameter(description = "ID da turma", example = "10") @PathVariable Long id
    ) {
        return ResponseEntity.ok(schoolRoomService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Criar turma",
            description = """
                    Cria uma nova turma vinculada a uma oficina, com grade semanal e dados de sala/capacidade.

                    **POST** `/api/v1/schoolrooms`

                    ## Autenticação
                    - JWT obrigatório.
                    - Papéis: **ADMIN** ou **SECRETARY**.

                    ## Corpo (`SchoolRoomCreateRequest`)
                    | Campo | Obrigatório | Descrição |
                    |-------|-------------|-----------|
                    | name | Sim | Nome da turma |
                    | scheduleSlots | Sim | Lista com ao menos um intervalo (`dayOfWeek`, `startTime`, `endTime` em HH:mm) |
                    | classroom | Sim | Identificação da sala |
                    | workshopId | Sim | ID da oficina |
                    | capacity | Sim | Capacidade (> 0) |
                    | teacherId | Não | Titular (pode ser definido depois via PUT `/teacher`) |

                    ## Observações
                    - **201** — turma criada
                    - **404** — oficina (`workshopId`) não existe
                    - **409** — já existe turma ativa com o mesmo nome na mesma oficina; ou oficina desativada (regra de negócio)
                    - **400** — validação (grade vazia, horários inválidos ou sobrepostos, etc.)
                    - **401** / **403** — autenticação ou papel insuficiente
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Turma criada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.SCHOOL_ROOM_JSON))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.VALIDATION_SCHOOLROOMS))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_SCHOOLROOMS_WRITE))),
            @ApiResponse(responseCode = "404", description = "Oficina não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_WORKSHOP_FOR_SCHOOLROOM))),
            @ApiResponse(responseCode = "409", description = "Conflito de regra de negócio",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_SCHOOLROOM_GENERIC)))
    })
    public ResponseEntity<SchoolRoomResponse> create(@Valid @RequestBody SchoolRoomCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolRoomService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Atualizar turma",
            description = """
                    Atualiza dados da turma, incluindo flag **active**, oficina, grade e professor titular.

                    **PUT** `/api/v1/schoolrooms/{id}`

                    ## Autenticação
                    - JWT + **ADMIN** ou **SECRETARY**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da turma |

                    ## Corpo (`SchoolRoomUpdateRequest`)
                    | Campo | Obrigatório | Descrição |
                    |-------|-------------|-----------|
                    | name | Sim | Nome da turma |
                    | scheduleSlots | Sim | Grade (ao menos um slot) |
                    | classroom | Sim | Sala |
                    | workshopId | Sim | Oficina |
                    | capacity | Sim | Capacidade |
                    | teacherId | Não | Titular |
                    | active | Sim | Se a turma está ativa |

                    ## Observações
                    - **200** — atualizada
                    - **404** — turma não existe ou oficina (`workshopId`) não existe
                    - **409** — nome duplicado na oficina; ou tentativa de vincular a oficina desativada
                    - **400** — validação da grade/horários
                    - **401** / **403** — autenticação ou papel
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turma atualizada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.SCHOOL_ROOM_JSON))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.VALIDATION_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "404", description = "Turma ou oficina não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_SCHOOLROOM))),
            @ApiResponse(responseCode = "409", description = "Conflito de regra de negócio",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_SCHOOLROOM_PUT)))
    })
    public ResponseEntity<SchoolRoomResponse> update(
            @Parameter(description = "ID da turma", example = "10") @PathVariable Long id,
            @Valid @RequestBody SchoolRoomUpdateRequest request
    ) {
        return ResponseEntity.ok(schoolRoomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Desativar turma",
            description = """
                    Marca a turma como inativa. Não remove registros históricos.

                    **DELETE** `/api/v1/schoolrooms/{id}`

                    ## Autenticação
                    - JWT + **ADMIN** ou **SECRETARY**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da turma |

                    ## Resposta de sucesso
                    - **204 No Content** — sem corpo

                    ## Observações
                    - **409** — turma ainda possui matrículas ativas
                    - **404** — turma não encontrada
                    - **401** / **403** — autenticação ou papel
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Turma desativada (sem corpo)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_SCHOOLROOM))),
            @ApiResponse(responseCode = "409", description = "Turma possui matrículas ativas",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_SCHOOLROOM_DEACTIVATE)))
    })
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID da turma", example = "10") @PathVariable Long id
    ) {
        schoolRoomService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Atribuir professor à turma",
            description = """
                    Define o professor titular (colaborador elegível) da turma.

                    **PUT** `/api/v1/schoolrooms/{id}/teacher`

                    ## Autenticação
                    - JWT + **ADMIN** ou **SECRETARY**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da turma |

                    ## Corpo (`AssignTeacherRequest`)
                    | Campo | Obrigatório | Descrição |
                    |-------|-------------|-----------|
                    | teacherId | Sim | ID do colaborador professor |

                    ## Observações
                    - **200** — turma retornada com titular atualizado
                    - **404** — turma ou professor não encontrado (ou colaborador inelegível)
                    - **409** — professor já é titular desta turma
                    - **400** — validação (`teacherId` nulo)
                    - **401** / **403** — autenticação ou papel
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor atribuído",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.SCHOOL_ROOM_JSON))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.VALIDATION_TEACHER_BODY))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "404", description = "Turma ou professor não encontrado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_TEACHER_ASSIGN))),
            @ApiResponse(responseCode = "409", description = "Professor já vinculado à turma",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_TEACHER_ALREADY)))
    })
    public ResponseEntity<SchoolRoomResponse> assignTeacher(
            @Parameter(description = "ID da turma", example = "10") @PathVariable Long id,
            @Valid @RequestBody AssignTeacherRequest request
    ) {
        return ResponseEntity.ok(schoolRoomService.assignTeacher(id, request));
    }

    @DeleteMapping("/{id}/teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Remover professor da turma",
            description = """
                    Remove o professor titular da turma (campo titular fica vazio).

                    **DELETE** `/api/v1/schoolrooms/{id}/teacher`

                    ## Autenticação
                    - JWT + **ADMIN** ou **SECRETARY**.

                    ## Parâmetros de path
                    | Parâmetro | Descrição |
                    |-----------|-----------|
                    | id | ID da turma |

                    ## Observações
                    - **200** — turma sem titular, corpo completo da turma
                    - **409** — turma não possui professor vinculado
                    - **404** — turma não encontrada
                    - **401** / **403** — autenticação ou papel
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor removido; turma atualizada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.SCHOOL_ROOM_JSON))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.UNAUTHORIZED_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.FORBIDDEN_SCHOOLROOMS_ID))),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.NOT_FOUND_SCHOOLROOM))),
            @ApiResponse(responseCode = "409", description = "Turma não possui professor vinculado",
                    content = @Content(examples = @ExampleObject(value = AcademicApiExamples.CONFLICT_TEACHER_NONE)))
    })
    public ResponseEntity<SchoolRoomResponse> removeTeacher(
            @Parameter(description = "ID da turma", example = "10") @PathVariable Long id
    ) {
        return ResponseEntity.ok(schoolRoomService.removeTeacher(id));
    }

    @GetMapping("/{id}/teacher-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Histórico de vínculos de professor na turma",
            description = """
                Retorna o histórico de atribuições e remoções de professor titular na turma,
                extraído da tabela `audit_log`. Útil para calcular chamadas pendentes
                a partir da data em que o professor foi vinculado.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Histórico de vínculos retornado com sucesso",
                    content = @Content(examples = @ExampleObject(value = """
                        [
                          {
                            "schoolRoomId": 3,
                            "teacherId": 7,
                            "teacherName": "Ricardo Mendes",
                            "changedBy": 1,
                            "changedAt": "2025-05-10T14:32:00"
                          },
                          {
                            "schoolRoomId": 3,
                            "teacherId": 4,
                            "teacherName": "Fernanda Oliveira",
                            "changedBy": 1,
                            "changedAt": "2025-04-01T09:15:00"
                          }
                        ]
                        """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado",
                    content = @Content(examples = @ExampleObject(value = """
                        {
                          "status": 401,
                          "error": "Unauthorized",
                          "message": "Token ausente ou inválido"
                        }
                        """))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(examples = @ExampleObject(value = """
                        {
                          "status": 403,
                          "error": "Forbidden",
                          "message": "Acesso negado"
                        }
                        """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Turma não encontrada",
                    content = @Content(examples = @ExampleObject(value = """
                        {
                          "status": 404,
                          "error": "Not Found",
                          "message": "Turma não encontrada"
                        }
                        """))
            )
    })
    public ResponseEntity<List<TeacherAssignmentLogResponse>> teacherAssignments(
            @Parameter(description = "ID da turma", example = "3") @PathVariable Long id
    ) {
        return ResponseEntity.ok(auditLogService.findTeacherAssignments(id));
    }
}
