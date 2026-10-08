package br.sing.academic.api;

import br.sing.academic.api.controller.SchoolRoomController;
import br.sing.academic.api.dto.response.SchoolRoomStudentResponse;
import br.sing.academic.application.EnrollmentQueryService;
import br.sing.academic.application.SchoolRoomService;
import br.sing.common.application.AuditLogService;
import br.sing.exception.BusinessException;
import br.sing.test.StandaloneMockMvcSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc standalone: o slice {@code @WebMvcTest} não registrava o mapeamento deste controller
 * (requisição caía em {@code ResourceHttpRequestHandler}). Aqui o controller é montado explicitamente.
 * <p>
 * {@code @PreAuthorize} do controller não é avaliado neste setup; a regra de perfil continua coberta
 * em {@link br.sing.academic.application.SchoolRoomServiceListStudentsTest} e o fluxo HTTP valida
 * o contrato + {@link GlobalExceptionHandler}.
 */
@ExtendWith(MockitoExtension.class)
class SchoolRoomControllerListStudentsWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private SchoolRoomService schoolRoomService;

    @Mock
    private EnrollmentQueryService enrollmentQueryService;
    @Mock
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        Mockito.reset(schoolRoomService, enrollmentQueryService, auditLogService);
        SchoolRoomController controller = new SchoolRoomController(
                schoolRoomService, enrollmentQueryService, auditLogService);
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(controller);
    }

    @Test
    void retorna403QuandoPerfilNaoAutorizadoNaCamadaServico() throws Exception {
        when(schoolRoomService.listStudents(eq(1L), any()))
                .thenThrow(new BusinessException("Acesso negado", HttpStatus.FORBIDDEN));
        mockMvc.perform(get("/api/v1/schoolrooms/1/students")
                        .with(user("aluno@test.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void retorna200QuandoAdmin() throws Exception {
        when(schoolRoomService.listStudents(eq(1L), any())).thenReturn(List.of(
                new SchoolRoomStudentResponse(5L, "Aluno", "aluno@sing.local", "119", "6", "MANHA", null, true)
        ));
        mockMvc.perform(get("/api/v1/schoolrooms/1/students")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(5L))
                .andExpect(jsonPath("$[0].name").value("Aluno"))
                .andExpect(jsonPath("$[0].enrollmentActive").value(true));
    }

    @Test
    void retorna404QuandoServicoLancaEntityNotFound() throws Exception {
        when(schoolRoomService.listStudents(eq(99L), any()))
                .thenThrow(new br.sing.exception.EntityNotFoundException("Turma não encontrada"));
        mockMvc.perform(get("/api/v1/schoolrooms/99/students")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void retorna403QuandoServicoLancaBusinessExceptionForbidden() throws Exception {
        when(schoolRoomService.listStudents(eq(1L), any()))
                .thenThrow(new BusinessException("Acesso negado", HttpStatus.FORBIDDEN));
        mockMvc.perform(get("/api/v1/schoolrooms/1/students")
                        .with(user("prof@test.com").roles("PROFESSOR")))
                .andExpect(status().isForbidden());
    }
}
