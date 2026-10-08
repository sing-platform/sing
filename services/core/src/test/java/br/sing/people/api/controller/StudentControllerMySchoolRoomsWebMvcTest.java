package br.sing.people.api.controller;

import br.sing.academic.application.EnrollmentQueryService;
import br.sing.exception.CustomAccessDeniedHandler;
import br.sing.exception.CustomAuthenticationEntryPoint;
import br.sing.iam.infra.JwtAuthenticationFilter;
import br.sing.people.application.StudentMeSchoolRoomService;
import br.sing.people.application.StudentService;
import br.sing.test.TestSecurityConfig;
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
