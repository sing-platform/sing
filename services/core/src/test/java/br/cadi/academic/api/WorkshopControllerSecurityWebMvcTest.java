package br.cadi.academic.api;

import br.cadi.academic.api.controller.WorkshopController;
import br.cadi.academic.api.dto.request.WorkshopCreateRequest;
import br.cadi.academic.application.WorkshopService;
import br.cadi.exception.CustomAccessDeniedHandler;
import br.cadi.exception.CustomAuthenticationEntryPoint;
import br.cadi.iam.infra.JwtAuthenticationFilter;
import br.cadi.test.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkshopController.class)
@Import(TestSecurityConfig.class)
class WorkshopControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkshopService workshopService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Test
    void createRetorna401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/api/v1/workshops")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkshopCreateRequest("Oficina A"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRetorna403QuandoPerfilSemPermissao() throws Exception {
        mockMvc.perform(post("/api/v1/workshops")
                        .with(user("sec@test.com").roles("SECRETARY"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkshopCreateRequest("Oficina A"))))
                .andExpect(status().isForbidden());
    }

}
