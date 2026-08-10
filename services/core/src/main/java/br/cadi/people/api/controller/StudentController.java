package br.cadi.people.api.controller;

import br.cadi.academic.api.dto.response.EnrollmentLinkResponse;
import br.cadi.academic.api.dto.response.SchoolRoomResponse;
import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.people.api.dto.StudentEnrollmentFilter;
import br.cadi.people.api.dto.request.StudentRequest;
import br.cadi.people.api.dto.response.StudentResponse;
import br.cadi.people.api.dto.request.StudentUpdateRequest;
import br.cadi.people.application.StudentMeSchoolRoomService;
import br.cadi.people.application.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/students")
@Tag(name = "Student", description = "Endpoints para gestão de alunos e seus vínculos")
public class StudentController {

    private final StudentService studentService;
    private final EnrollmentQueryService enrollmentQueryService;
    private final StudentMeSchoolRoomService studentMeSchoolRoomService;

    @GetMapping("/me/schoolrooms")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    @Operation(summary = "Minhas turmas matriculadas", description = "Aluno: turmas com matrícula ativa. Responsável: união das turmas dos dependentes.")
    @ApiResponse(responseCode = "200", description = "Lista de turmas (dados de grade e professor)")
    public ResponseEntity<List<SchoolRoomResponse>> myEnrolledSchoolRooms() {
        return ResponseEntity.ok(studentMeSchoolRoomService.listMyEnrolledSchoolRooms());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Cadastrar novo aluno e responsáveis",
            description = """
                Cadastra um estudante completo, incluindo dados escolares, endereço e vínculo com responsáveis.
                
                ## Restrições
                - Requer autenticação com token de **ROLE_ADMIN** ou **ROLE_SECRETARY**
                - O CPF do aluno e dos responsáveis deve ser único no sistema.
                - Inclua o token no header: `Authorization: Bearer {token}`
                
                ## Regras de Negócio
                | Campo | Regra |
                |-------|-----------|
                | **guardians** | Pelo menos um responsável deve ser enviado e marcado como `isPrimary: true`. |
                | **grade** | Deve seguir o padrão de códigos (ex: `1-ef`, `3-em`). |
                | **password** | Se não enviado, o padrão será `Cadi@123`. |
                | **cpf** | Deve conter apenas os 11 números, sem pontos ou traços. |
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Estudante criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos enviados"),
            @ApiResponse(responseCode = "409", description = "Conflito: CPF ou E-mail já existentes")
    })
    public ResponseEntity<StudentResponse> create(@RequestBody @Valid StudentRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Listar alunos (paginado)",
            description = "Paginação Spring (`page`, `size`, `sort`). Filtros opcionais: `search` (nome), `periodStudies` (trecho do período na escola principal), `enrollmentFilter` (ANY|NONE|ONE|TWO matrículas ativas)."
    )
    @ApiResponse(responseCode = "200", description = "Página de estudantes (`content`, `totalElements`, …)")
    public ResponseEntity<Page<StudentResponse>> pageStudents(
            @Parameter(description = "Trecho do nome (case-insensitive)") @RequestParam(required = false) String search,
            @Parameter(description = "Trecho de periodStudies (ex.: MATUTINO, VESPERTINO)") @RequestParam(required = false) String periodStudies,
            @Parameter(description = "Filtro pela quantidade de matrículas ativas") @RequestParam(required = false) StudentEnrollmentFilter enrollmentFilter,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(studentService.pageForManagement(search, periodStudies, enrollmentFilter, pageable));
    }

    @GetMapping("/{studentId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'PROFESSOR')")
    @Operation(summary = "Listar matrículas do aluno", description = "Busca de forma paginada todas as turmas em que o aluno possui vínculo (matrícula).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de matrículas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para visualizar matrículas"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    public ResponseEntity<Page<EnrollmentLinkResponse>> pageEnrollmentsByStudent(
            @Parameter(description = "ID do aluno para busca") @PathVariable Long studentId,
            @Parameter(description = "Filtrar apenas matrículas ativas (true) ou inativas (false)") @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "enrollmentDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                enrollmentQueryService.pageByStudent(studentId, active, pageable, authentication)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(summary = "Buscar aluno por ID", description = "Retorna os dados detalhados de um estudante específico através do seu identificador único.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudante encontrado"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado no banco de dados")
    })
    public ResponseEntity<StudentResponse> findById(@Parameter(description = "ID do estudante") @PathVariable Long id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Atualizar dados do aluno",
            description = """
                Atualiza as informações cadastrais de um aluno existente.
                
                ## Regras de Atualização
                - **Campo Imutável:** Apenas o **cpf** não pode ser alterado após o cadastro para garantir a integridade histórica do registro.
                - **E-mail e Dados Pessoais:** Podem ser editados normalmente. O sistema validará se o novo e-mail já não pertence a outro usuário.
                
                
                ## Parâmetros de Requisição
                | Parâmetro | Tipo | Descrição |
                |-----------|------|-----------|
                | **id** | `Long` | ID único do aluno (Path Variable). |
                | **dto** | `JSON` | Corpo da requisição com os campos a serem atualizados. |
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação (Ex: e-mail inválido)"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado"),
            @ApiResponse(responseCode = "409", description = "O novo e-mail informado já está em uso")
    })
    public ResponseEntity<StudentResponse> update(@PathVariable Long id, @RequestBody @Valid StudentUpdateRequest dto) {
        return ResponseEntity.ok(studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(summary = "Excluir aluno", description = "Remove permanentemente o registro do estudante e seus vínculos do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Estudante removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "ID do estudante não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }


}