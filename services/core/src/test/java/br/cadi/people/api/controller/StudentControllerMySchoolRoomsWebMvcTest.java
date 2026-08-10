package br.cadi.people.api.controller;

import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.exception.CustomAccessDeniedHandler;
import br.cadi.exception.CustomAuthenticationEntryPoint;
import br.cadi.iam.infra.JwtAuthenticationFilter;
import br.cadi.people.application.StudentMeSchoolRoomService;
import br.cadi.people.application.StudentService;
import br.cadi.test.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StudentController.class)
@Import(TestSecurityConfig.class)
class StudentControllerMySchoolRoomsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;
    @MockBean
    private EnrollmentQueryService enrollmentQueryService;
    @MockBean
    private StudentMeSchoolRoomService studentMeSchoolRoomService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Test
    void meSchoolroomsRetorna200ParaAluno() throws Exception {
        when(studentMeSchoolRoomService.listMyEnrolledSchoolRooms())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/v1/students/me/schoolrooms")
                        .with(user("aluno@test.com").roles("STUDENT")))
                .andExpect(status().isOk());
    }
}
