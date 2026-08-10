package br.cadi.people.api.controller;

import br.cadi.academic.application.AcademicAuthorizationService;
import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.academic.application.SchoolRoomService;
import br.cadi.exception.CustomAccessDeniedHandler;
import br.cadi.exception.CustomAuthenticationEntryPoint;
import br.cadi.iam.infra.JwtAuthenticationFilter;
import br.cadi.test.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TeacherController.class)
@Import(TestSecurityConfig.class)
class TeacherControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchoolRoomService schoolRoomService;
    @MockBean
    private EnrollmentQueryService enrollmentQueryService;
    @MockBean
    private AcademicAuthorizationService academicAuthorizationService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Test
    void pageMySchoolRoomsRetorna401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me/schoolrooms")
                        .with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pageMySchoolRoomsRetorna403QuandoPerfilNaoProfessor() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me/schoolrooms")
                        .with(user("sec@test.com").roles("SECRETARY")))
                .andExpect(status().isForbidden());
    }
}
