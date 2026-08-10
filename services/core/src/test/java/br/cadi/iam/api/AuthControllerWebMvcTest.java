package br.cadi.iam.api;

import br.cadi.exception.EntityNotFoundException;
import br.cadi.test.StandaloneMockMvcSupport;
import br.cadi.exception.UnauthorizedException;
import br.cadi.exception.UserCreationConflict;
import br.cadi.iam.application.AdminUserService;
import br.cadi.iam.application.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes WebMvc isolados (standaloneSetup) para {@link AuthController}.
 *
 * <p>Cobre status codes, validações de body e tradução de exceções de domínio
 * via {@link GlobalExceptionHandler}. Não exercita autorização (que é coberta
 * pelo SecurityConfig de produção e pelo br.cadi.test.TestSecurityConfig em outros testes).
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = StandaloneMockMvcSupport.objectMapper();

    @Mock
    private AuthService authService;
    @Mock
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        Mockito.reset(authService, adminUserService);
        AuthController controller = new AuthController(authService, adminUserService);
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(controller);
    }

    // ------------------------------------------------------------------
    // /register
    // ------------------------------------------------------------------

    @Test
    void registerRetorna200ComToken() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(AuthResponse.builder().token("jwt-token").build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("joao", "joao@cadi.com", "senha123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void registerRetorna400QuandoBodyInvalido() throws Exception {
        // password ausente — viola @NotBlank/@Size
        String invalidBody = "{\"username\":\"x\",\"email\":\"e@e.com\",\"password\":\"\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRetorna409QuandoEmailJaCadastrado() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserCreationConflict("Email already existente"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("joao", "joao@cadi.com", "senha123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ------------------------------------------------------------------
    // /authenticate
    // ------------------------------------------------------------------

    @Test
    void authenticateRetorna200ComToken() throws Exception {
        when(authService.authenticate(any(AuthRequest.class)))
                .thenReturn(AuthResponse.builder().token("jwt-token").build());

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("joao@cadi.com", "senha123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void authenticateRetorna400QuandoEmailVazio() throws Exception {
        String invalidBody = "{\"email\":\"\",\"password\":\"x\"}";

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType("application/json")
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticateRetorna401QuandoCredenciaisInvalidas() throws Exception {
        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new UnauthorizedException("invalid"));

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("a@a.com", "x"))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // /admin/cadastrar
    // ------------------------------------------------------------------

    @Test
    void adminCadastrarRetorna404QuandoRoleNaoExiste() throws Exception {
        when(adminUserService.cadastrarComRole(any(RegisterRequestAdmin.class)))
                .thenThrow(new EntityNotFoundException("Role não encontrada: ROLE_X"));

        String body = """
                {
                  "username": "novo",
                  "email": "novo@cadi.com",
                  "password": "senha123",
                  "role": "ROLE_X"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/admin/cadastrar")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
