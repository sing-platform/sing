package br.cadi.people.api.controller;

import br.cadi.exception.CustomAccessDeniedHandler;
import br.cadi.exception.CustomAuthenticationEntryPoint;
import br.cadi.iam.infra.JwtAuthenticationFilter;
import br.cadi.people.application.CollaboratorService;
import br.cadi.test.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CollaboratorController.class)
@Import(TestSecurityConfig.class)
class CollaboratorControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CollaboratorService collaboratorService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Test
    void listCollaboratorsRetorna401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/collaborators")
                        .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCollaboratorsRetorna403QuandoPerfilNaoAutorizado() throws Exception {
        mockMvc.perform(get("/api/v1/collaborators")
                        .with(user("prof@test.com").roles("PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCollaboratorsRetorna200ParaSecretary() throws Exception {
        mockMvc.perform(get("/api/v1/collaborators")
                        .with(user("sec@test.com").roles("SECRETARY"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
