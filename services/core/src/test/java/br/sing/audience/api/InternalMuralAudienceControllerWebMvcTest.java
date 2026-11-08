package br.sing.audience.api;

import br.sing.audience.application.MuralAudienceService;
import br.sing.exception.BusinessException;
import br.sing.exception.CustomAccessDeniedHandler;
import br.sing.exception.CustomAuthenticationEntryPoint;
import br.sing.iam.infra.JwtAuthenticationFilter;
import br.sing.test.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalMuralAudienceController.class)
@Import(TestSecurityConfig.class)
class InternalMuralAudienceControllerWebMvcTest {

    private static final String URL = "/api/v1/internal/mural/destinatarios";
    private static final String BODY = "{\"audienceRoles\":[\"ROLE_STUDENT\"],\"audienceSchoolroomIds\":[3]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MuralAudienceService audienceService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @BeforeEach
    void passaPeloFiltroJwt() throws Exception {
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<jakarta.servlet.FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void retorna401SemAutenticacao() throws Exception {
        mockMvc.perform(post(URL).contentType("application/json").content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(audienceService);
    }

    @Test
    void retorna403ParaPapeisSemPermissao() throws Exception {
        for (String role : List.of("STUDENT", "PARENT", "MANAGER", "PSYCHOLOGIST")) {
            mockMvc.perform(post(URL)
                            .with(user("x@sing.local").roles(role))
                            .contentType("application/json")
                            .content(BODY))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(audienceService);
    }

    @Test
    void retorna200ParaProfessorSecretariaEAdmin() throws Exception {
        when(audienceService.resolve(any(), any())).thenReturn(new AudienceResponse(
                new AudienceResponse.Author(1L, "Ana Souza", "admin@sing.local", "ROLE_ADMIN"),
                List.of(new AudienceResponse.Recipient(7L, "aluno@sing.local"))));

        for (String role : List.of("PROFESSOR", "SECRETARY", "ADMIN")) {
            mockMvc.perform(post(URL)
                            .with(user("autor@sing.local").roles(role))
                            .contentType("application/json")
                            .content(BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.author.userId").value(1))
                    .andExpect(jsonPath("$.author.name").value("Ana Souza"))
                    .andExpect(jsonPath("$.author.email").value("admin@sing.local"))
                    .andExpect(jsonPath("$.author.role").value("ROLE_ADMIN"))
                    .andExpect(jsonPath("$.recipients[0].userId").value(7))
                    .andExpect(jsonPath("$.recipients[0].email").value("aluno@sing.local"));
        }
    }

    @Test
    void repassaEmailDoAutorEPublicoAoServico() throws Exception {
        when(audienceService.resolve(any(), any())).thenReturn(new AudienceResponse(
                new AudienceResponse.Author(1L, "Ana", "autor@sing.local", "ROLE_ADMIN"), List.of()));

        mockMvc.perform(post(URL)
                        .with(user("autor@sing.local").roles("ADMIN"))
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(audienceService).resolve(
                eq("autor@sing.local"),
                eq(new AudienceRequest(Set.of("ROLE_STUDENT"), Set.of(3L))));
    }

    @Test
    void propagaErrosDeRegraComStatusDoServico() throws Exception {
        when(audienceService.resolve(any(), any())).thenThrow(new BusinessException(
                "Informe ao menos um campo de público-alvo: audienceRoles ou audienceSchoolroomIds.",
                HttpStatus.BAD_REQUEST));

        mockMvc.perform(post(URL)
                        .with(user("autor@sing.local").roles("ADMIN"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Informe ao menos um campo de público-alvo: audienceRoles ou audienceSchoolroomIds."));
    }
}
