package br.cadi.people.api.controller;

import br.cadi.exception.ApiError;
import br.cadi.people.api.dto.request.StudentPreRegisterCreateRequest;
import br.cadi.people.api.dto.request.StudentPreRegisterUpdateRequest;
import br.cadi.people.api.dto.response.StudentPreRegisterResponse;
import br.cadi.people.api.dto.response.WaitingListVacancyAlertResponse;
import br.cadi.people.application.StudentPreRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@RestController
@RequestMapping("/api/v1/student-pre-registers")
@RequiredArgsConstructor
@Tag(name = "Student Pre-Register", description = "Pre-registered students (waiting list)")
public class StudentPreRegisterController {

    private final StudentPreRegisterService studentPreRegisterService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Cadastrar candidato na lista de espera (pré-cadastro)",
            description = """
                Registra um candidato a aluno na fila de pré-cadastro, antes da matrícula formal no sistema.

                **POST** `/api/v1/student-pre-registers`

                ## Autenticação
                - Requer JWT no header `Authorization: Bearer {token}`
                - Papéis permitidos: **ROLE_ADMIN** ou **ROLE_SECRETARY**

                ## Corpo (JSON)
                | Campo | Obrigatório | Descrição |
                |-------|-------------|-----------|
                | name | Sim | Nome completo do candidato |
                | email | Sim | E-mail de contato (formato válido) |
                | phone | Sim | Telefone de contato |
                | workshopId | Sim | ID da oficina de interesse (deve existir em `/api/v1/workshops`) |
                | shift | Sim | Turno de preferência: `MORNING`, `AFTERNOON` ou `EVENING` |

                ## Regras de negócio
                - `workshopId` é validado: retorna **404** se a oficina não existir.
                - `createdAt` é preenchido automaticamente no servidor.
                - Não cria usuário de login nem matrícula; apenas o registro na lista de espera.

                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pré-cadastro criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos enviados"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN ou SECRETARY)"),
            @ApiResponse(responseCode = "404", description = "Oficina de interesse não encontrada")
    })
    public ResponseEntity<StudentPreRegisterResponse> create(
            @RequestBody @Valid StudentPreRegisterCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentPreRegisterService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all student pre-registers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pré-cadastros retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<StudentPreRegisterResponse>> findAll() {
        return ResponseEntity.ok(studentPreRegisterService.findAll());
    }

    @GetMapping("/vacancy-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(summary = "Listar alertas de vaga disponível com candidatos na lista de espera")
    public ResponseEntity<List<WaitingListVacancyAlertResponse>> listVacancyAlerts() {
        return ResponseEntity.ok(studentPreRegisterService.listVacancyAlerts());
    }

    @PostMapping("/{id}/send-summons")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(
            summary = "Enviar e-mail de convocação ao responsável",
            description = "Envia e-mail informando vaga disponível. Requer vaga aberta para a oficina/turno do candidato."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convocação enviada"),
            @ApiResponse(responseCode = "400", description = "Sem vaga disponível"),
            @ApiResponse(responseCode = "404", description = "Pré-cadastro não encontrado")
    })
    public ResponseEntity<Void> sendSummons(@PathVariable Long id, Authentication authentication) {
        studentPreRegisterService.sendSummons(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student pre-register by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<StudentPreRegisterResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(studentPreRegisterService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(summary = "Update a student pre-register")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<StudentPreRegisterResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid StudentPreRegisterUpdateRequest request
    ) {
        return ResponseEntity.ok(studentPreRegisterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(summary = "Delete a student pre-register")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentPreRegisterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
