package br.cadi.academic.api;

import br.cadi.academic.api.controller.SchoolRoomController;
import br.cadi.academic.api.dto.request.ScheduleSlotRequest;
import br.cadi.academic.api.dto.request.SchoolRoomCreateRequest;
import br.cadi.academic.api.dto.response.AdminScheduleBoardResponse;
import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.academic.application.SchoolRoomService;
import br.cadi.common.application.AuditLogService;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SchoolRoomController.class)
@Import(TestSecurityConfig.class)
class SchoolRoomControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SchoolRoomService schoolRoomService;
    @MockBean
    private EnrollmentQueryService enrollmentQueryService;
    @MockBean
    private AuditLogService auditLogService;
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
        var request = new SchoolRoomCreateRequest("Turma A", validSlots(), "Sala 1", 1L, 30, null);
        mockMvc.perform(post("/api/v1/schoolrooms")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRetorna403QuandoPerfilSemPermissao() throws Exception {
        var request = new SchoolRoomCreateRequest("Turma A", validSlots(), "Sala 1", 1L, 30, null);
        mockMvc.perform(post("/api/v1/schoolrooms")
                        .with(user("prof@test.com").roles("PROFESSOR"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listStudentsRetorna403QuandoPerfilAluno() throws Exception {
        mockMvc.perform(get("/api/v1/schoolrooms/1/students")
                        .with(user("aluno@test.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void scheduleBoardRetorna200ParaGestor() throws Exception {
        when(schoolRoomService.getAdminScheduleBoardData())
                .thenReturn(new AdminScheduleBoardResponse(List.of(), Map.of()));
        mockMvc.perform(get("/api/v1/schoolrooms/schedule-board")
                        .with(user("gestor@test.com").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    private List<ScheduleSlotRequest> validSlots() {
        return List.of(new ScheduleSlotRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));
    }
}
