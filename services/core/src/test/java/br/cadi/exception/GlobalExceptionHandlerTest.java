package br.cadi.exception;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import br.cadi.test.StandaloneMockMvcSupport;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes para {@link GlobalExceptionHandler}.
 *
 * <p>Usa um controller fake que apenas relança exceções de domínio para
 * exercitar os {@code @ExceptionHandler} configurados.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(new TestController());
    }

    @Test
    void businessExceptionRetornaStatusCustomizadoComBodyApiError() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("conflito de regra"))
                .andExpect(jsonPath("$.path").value("/test/business"));
    }

    @Test
    void entityNotFoundExceptionRetorna404() throws Exception {
        mockMvc.perform(get("/test/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("entidade ausente"));
    }

    @Test
    void userCreationConflictRetorna409() throws Exception {
        mockMvc.perform(get("/test/user-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void serverErrorRetorna500() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void excecaoGenericaRetorna500ComMensagemSegura() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred. Please try again later."));
    }

    @Test
    void httpMessageNotReadableRetorna400() throws Exception {
        mockMvc.perform(post("/test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void methodNotSupportedRetorna405() throws Exception {
        // /test/echo é POST; PUT deve gerar 405
        mockMvc.perform(put("/test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void methodArgumentNotValidRetorna400ComLogDeFields() throws Exception {
        // body com name vazio dispara MethodArgumentNotValidException via @Valid
        mockMvc.perform(post("/test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fields").isArray())
                .andExpect(jsonPath("$.fields[0].field").value("name"));
    }

    // ------------------------------------------------------------------
    // Controller fake usado apenas pelos testes acima
    // ------------------------------------------------------------------

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        public String business() {
            throw new BusinessException("conflito de regra", HttpStatus.CONFLICT);
        }

        @GetMapping("/notfound")
        public String notFound() {
            throw new EntityNotFoundException("entidade ausente");
        }

        @GetMapping("/user-conflict")
        public String userConflict() {
            throw new UserCreationConflict("email já cadastrado");
        }

        @GetMapping("/server-error")
        public String serverError() {
            throw new ServerError("falha interna");
        }

        @GetMapping("/generic-error")
        public String genericError() {
            throw new RuntimeException("boom");
        }

        @PostMapping("/echo")
        public Echo echo(@jakarta.validation.Valid @RequestBody Echo body) {
            return body;
        }

        record Echo(@NotBlank(message = "name é obrigatório") String name) {}
    }
}
