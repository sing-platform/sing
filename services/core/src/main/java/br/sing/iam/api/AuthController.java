package br.sing.iam.api;

import br.sing.exception.ApiError;
import br.sing.iam.api.examples.AuthApiExamples;
import br.sing.iam.application.AuthService;
import br.sing.iam.application.AdminUserService;
import br.sing.iam.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para autenticação e registro de usuários")
public class AuthController {

    private final AuthService service;
    private final AdminUserService adminUserService;

    @Operation(
            summary = "Registrar novo usuário",
            description = """
                    Cria um novo usuário com role padrão **ROLE_ALUNO** e retorna um token JWT.
                    
                    ## Campos obrigatórios
                    | Campo | Descrição |
                    |-------|-----------|
                    | username | Nome de usuário único |
                    | email | Email válido e único |
                    | password | Senha do usuário |
                    
                    ## Observação
                    - O registro público sempre atribui **ROLE_ALUNO**
                    - Para cadastrar com outra role, use o endpoint `/admin/cadastrar` com token de admin
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no body", content = @Content(examples = @ExampleObject(value = "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"error\": null,\n" +
                    "  \"message\": \"Validation failed\",\n" +
                    "  \"timestamp\": null,\n" +
                    "  \"path\": \"/api/v1/auth/register\",\n" +
                    "  \"details\": [\n" +
                    "    \"password: Password is required\"\n" +
                    "  ]\n" +
                    "}"))),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado", content = @Content(examples = @ExampleObject(value = "{\n" +
                    "  \"status\": 409,\n" +
                    "  \"error\": \"Conflict\",\n" +
                    "  \"message\": \"Email already existente\",\n" +
                    "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                    "  \"path\": \"/api/v1/auth/register\",\n" +
                    "  \"details\": null\n" +
                    "}")))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Autentica com email e senha e retorna um token JWT.
                    
                    ## Como usar o token
                    1. Copie o token retornado
                    2. Clique em **Authorize** (cadeado) no topo da página
                    3. Digite `Bearer {seu_token}` e confirme
                    
                    ## Usuários padrão (perfil dev, seed)
                    Senha comum: **SING@123**
                    
                    | E-mail | Role |
                    |--------|------|
                    | admin@sing.local | ROLE_ADMIN |
                    | teacher@sing.local | ROLE_PROFESSOR |
                    | student@sing.local | ROLE_STUDENT |
                    | parent@sing.local | ROLE_PARENT |
                    | secretary@sing.local | ROLE_SECRETARY |
                    | psychologist@sing.local | ROLE_PSYCHOLOGIST |
                    | financeiro@sing.local | ROLE_MANAGER |
                    
                    No **Try it out**, use os exemplos pré-preenchidos no body (Admin, Professor, Aluno, etc.).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no body", content = @Content(examples = @ExampleObject(
                    "{\n" +
                            "  \"status\": 400,\n" +
                            "  \"error\": null,\n" +
                            "  \"message\": \"Validation failed\",\n" +
                            "  \"timestamp\": null,\n" +
                            "  \"path\": \"/api/v1/auth/authenticate\",\n" +
                            "  \"details\": [\n" +
                            "    \"password: Senha obrigatoria\"\n" +
                            "  ]\n" +
                            "}"
            ))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content(examples = @ExampleObject(
                    "{\n" +
                            "  \"status\": 401,\n" +
                            "  \"error\": null,\n" +
                            "  \"message\": \"Unauthorized\",\n" +
                            "  \"timestamp\": 2026-03-27T14:44:25.3132291,\n" +
                            "  \"path\": \"/api/v1/auth/authenticate\",\n" +
                            "  \"details\": [\n" +
                            "  ]\n" +
                            "}"
            )))
    })
    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(
            @Valid @org.springframework.web.bind.annotation.RequestBody
            @RequestBody(
                    description = "Credenciais de login (seed dev — senha padrão: SING@123)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthRequest.class),
                            examples = {
                                    @ExampleObject(name = "Admin", value = AuthApiExamples.AUTH_ADMIN),
                                    @ExampleObject(name = "Professor", value = AuthApiExamples.AUTH_PROFESSOR),
                                    @ExampleObject(name = "Aluno", value = AuthApiExamples.AUTH_STUDENT),
                                    @ExampleObject(name = "Responsável", value = AuthApiExamples.AUTH_PARENT),
                                    @ExampleObject(name = "Secretaria", value = AuthApiExamples.AUTH_SECRETARY),
                                    @ExampleObject(name = "Psicólogo", value = AuthApiExamples.AUTH_PSYCHOLOGIST),
                                    @ExampleObject(name = "Financeiro", value = AuthApiExamples.AUTH_FINANCE)
                            }
                    )
            )
            AuthRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @Operation(summary = "Alterar a senha do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @org.springframework.web.bind.annotation.RequestBody ChangePasswordRequest request
    ) {
        service.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Cadastrar usuário com role específica",
            description = """
                    Cadastra um novo usuário com uma role específica.
                    
                    ## Restrições
                    - Requer autenticação com token de **ROLE_ADMIN**
                    - Inclua o token no header: `Authorization: Bearer {token}`
                    
                    ## Roles disponíveis
                    | Role | Descrição |
                    |------|-----------|
                    | ROLE_ADMIN | Acesso total ao sistema |
                    | ROLE_PROFESSOR | Acesso a recursos de professor |
                    | ROLE_ALUNO | Acesso básico de aluno |
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no body",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 400,\n" +
                            "  \"error\": \"Bad Request\",\n" +
                            "  \"message\": \"Invalid request body\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/auth/cadastrar\",\n" +
                            "  \"details\": \"Validation errors here\"\n" +
                            "}"))
            ),
            @ApiResponse(responseCode = "403", description = "Acesso negado — requer ROLE_ADMIN",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 403,\n" +
                            "  \"error\": \"Forbidden\",\n" +
                            "  \"message\": \"Access denied. Required role: ROLE_ADMIN\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/auth/cadastrar\"\n" +
                            "}"))
            ), @ApiResponse(responseCode = "404", description = "Role não encontrada", content = @Content(examples = @ExampleObject(value = "{\n" +
            "  \"status\": 404,\n" +
            "  \"error\": \"Resource not Found\",\n" +
            "  \"message\": \"Given role dont exists\",\n" +
            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
            "  \"path\": \"/api/v1/auth/cadastrar\",\n" +
            "  \"details\": string\n" +
            "}"))),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado", content = @Content(examples = @ExampleObject(value = "{\n" +
                    "  \"status\": 409,\n" +
                    "  \"error\": \"Conflict\",\n" +
                    "  \"message\": \"Email already existente\",\n" +
                    "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                    "  \"path\": \"/api/v1/auth/cadastrar\",\n" +
                    "  \"details\": string\n" +
                    "}")))
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/cadastrar")
    public ResponseEntity<AuthResponse> cadastrar(
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequestAdmin request) {
        return ResponseEntity.ok(adminUserService.cadastrarComRole(request));
    }

    @Operation(
            summary = "Listar roles disponíveis",
            description = "Retorna todas as roles (papéis) cadastradas no sistema. Requer autenticação com ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles retornadas com sucesso",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 200,\n" +
                            "  \"message\": \"Roles fetched successfully\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/admin/roles\",\n" +
                            "  \"data\": [\n" +
                            "    {\n" +
                            "      \"id\": 1,\n" +
                            "      \"name\": \"ROLE_ADMIN\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"id\": 2,\n" +
                            "      \"name\": \"ROLE_USER\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"))
            ),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 401,\n" +
                            "  \"error\": \"Unauthorized\",\n" +
                            "  \"message\": \"Authentication is required to access this resource\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/admin/roles\"\n" +
                            "}"))
            ),
            @ApiResponse(responseCode = "403", description = "Acesso negado — requer ROLE_ADMIN",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 403,\n" +
                            "  \"error\": \"Forbidden\",\n" +
                            "  \"message\": \"Access denied. Required role: ROLE_ADMIN\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/admin/roles\"\n" +
                            "}"))
            )
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/roles")
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(service.getAllRoles());
    }

    @Operation(
            summary = "Atribuir role a um usuário",
            description = """
                    Altera a role de um usuário existente.
                    
                    ## Restrições
                    - Requer autenticação com token de **ROLE_ADMIN**
                    
                    ## Roles disponíveis
                    | Role | Descrição |
                    |------|-----------|
                    | ROLE_ADMIN | Acesso total ao sistema |
                    | ROLE_PROFESSOR | Acesso a recursos de professor |
                    | ROLE_ALUNO | Acesso básico de aluno |
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role atribuída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no body", content = @Content(examples = @ExampleObject(
                    "{\n" +
                            "  \"status\": 400,\n" +
                            "  \"error\": null,\n" +
                            "  \"message\": \"Validation failed\",\n" +
                            "  \"timestamp\": null,\n" +
                            "  \"path\": \"/api/v1/auth/admin/atribuir-role\",\n" +
                            "  \"details\": [\n" +
                            "    \"password: Senha obrigatoria\"\n" +
                            "  ]\n" +
                            "}"
            ))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 401,\n" +
                            "  \"error\": \"Unauthorized\",\n" +
                            "  \"message\": \"Authentication is required to access this resource\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/auth/admin/atribuir-role\"\n" +
                            "}"))
            ),
            @ApiResponse(responseCode = "403", description = "Acesso negado — requer ROLE_ADMIN",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 403,\n" +
                            "  \"error\": \"Forbidden\",\n" +
                            "  \"message\": \"Access denied. Required role: ROLE_ADMIN\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/admin/roles\"\n" +
                            "}"))
            ),
            @ApiResponse(responseCode = "404", description = "Usuário ou role não encontrados",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 404,\n" +
                            "  \"error\": \"Resource not Found\",\n" +
                            "  \"message\": \"User or role not found\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/...\",\n" +
                            "  \"details\": \"The requested user osr role does not exit\"\n" +
                            "}"))
            )})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/atribuir-role")
    public ResponseEntity<UserDTO> setRole(
            @org.springframework.web.bind.annotation.RequestBody AtribuirRoleRequest request) {
        return ResponseEntity.ok(service.setRole(request.username(), request.role()));
    }


    @Operation(
            summary = "Listar todos os usuários",
            description = """
                    Retorna todos os usuários cadastrados no sistema com suas respectivas roles.
                    
                    ## Restrições
                    - Requer autenticação com token de **ROLE_ADMIN**
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuários retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — requer ROLE_ADMIN",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 403,\n" +
                            "  \"error\": \"Forbidden\",\n" +
                            "  \"message\": \"Access denied. Required role: ROLE_ADMIN\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/admin/roles\"\n" +
                            "}"))
            ),            @ApiResponse(responseCode = "401", description = "Autenticação necessária",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"status\": 401,\n" +
                            "  \"error\": \"Unauthorized\",\n" +
                            "  \"message\": \"Authentication is required to access this resource\",\n" +
                            "  \"timestamp\": \"2026-03-27T14:40:32.1434074\",\n" +
                            "  \"path\": \"/api/v1/auth/admin/atribuir-role\"\n" +
                            "}"))
            ),    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/all-users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }
}