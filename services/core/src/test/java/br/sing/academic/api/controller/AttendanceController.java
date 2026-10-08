package br.sing.academic.api.controller;

import br.sing.academic.application.AttendanceService;
import br.sing.academic.application.AttendanceSummaryService;
import br.sing.common.application.AuditLogService;
import br.sing.academic.api.dto.request.AttendanceItemRequest;
import br.sing.academic.api.dto.request.BatchAttendanceRequest;
import br.sing.academic.api.dto.response.AttendanceItemResponse;
import br.sing.academic.api.dto.response.LectureCallResponse;
import br.sing.academic.domain.enums.AttendanceStatus;
import br.sing.exception.BusinessRuleException;
import br.sing.exception.ForbiddenException;
import br.sing.test.StandaloneMockMvcSupport;
import br.sing.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceController — testes de endpoints")
class AttendanceControllerWebMvcTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private AttendanceSummaryService attendanceSummaryService;
    @Mock
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        Mockito.reset(attendanceService, attendanceSummaryService, auditLogService);

        AttendanceController controller = new AttendanceController(
                attendanceService, attendanceSummaryService, auditLogService);

        mockMvc = StandaloneMockMvcSupport.standaloneSetup(controller);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ── GET /call/schoolroom/{id} ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/attendance/call/schoolroom/{schoolroomId}")
    class StartCall {

        @Test
        @DisplayName("retorna 200 com lista de alunos para professor da turma")
        void retorna200ComListaDeAlunos() throws Exception {
            var response = new LectureCallResponse(
                    1L, "Tópico A", LocalDateTime.now(), false,
                    List.of(new AttendanceItemResponse(null, 100L, "Lucas Souza", AttendanceStatus.PRESENT, null)),
                    true
            );
            when(attendanceService.startCall(eq(1L), any(LocalDate.class))).thenReturn(response);

            mockMvc.perform(get("/api/v1/attendance/call/schoolroom/1")
                            .param("date", LocalDate.now().toString())
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lectureId").value(1L))
                    .andExpect(jsonPath("$.closed").value(false))
                    .andExpect(jsonPath("$.attendances[0].studentName").value("Lucas Souza"))
                    .andExpect(jsonPath("$.attendances[0].status").value("PRESENT"));
        }

        @Test
        @DisplayName("retorna 200 com status null quando não há chamada no dia")
        void retorna200ComStatusNullQuandoSemChamada() throws Exception {
            var response = new LectureCallResponse(
                    null, null, null, false,
                    List.of(new AttendanceItemResponse(null, 100L, "Lucas Souza", null, null)),
                    false
            );
            when(attendanceService.startCall(eq(1L), any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/attendance/call/schoolroom/1")
                            .param("date", LocalDate.now().toString())
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attendances[0].status").isEmpty());
        }

        @Test
        @DisplayName("retorna 403 quando professor tenta ver turma que não é sua")
        void retorna403QuandoProfessorSemPermissao() throws Exception {
            when(attendanceService.startCall(eq(1L), any()))
                    .thenThrow(new ForbiddenException("Você não tem permissão"));

            mockMvc.perform(get("/api/v1/attendance/call/schoolroom/1")
                            .param("date", LocalDate.now().toString())
                            .with(user("outro@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("retorna 404 quando turma não existe")
        void retorna404QuandoTurmaNaoExiste() throws Exception {
            when(attendanceService.startCall(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Turma não encontrada", 99L));

            mockMvc.perform(get("/api/v1/attendance/call/schoolroom/99")
                            .param("date", LocalDate.now().toString())
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("usa data de hoje quando parâmetro date não é enviado")
        void usaHojeQuandoDateNaoEnviado() throws Exception {
            var response = new LectureCallResponse(null, null, null, false, List.of(), false);
            when(attendanceService.startCall(eq(1L), eq(LocalDate.now()))).thenReturn(response);

            mockMvc.perform(get("/api/v1/attendance/call/schoolroom/1")
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isOk());

            verify(attendanceService).startCall(eq(1L), eq(LocalDate.now()));
        }
    }

    // ── POST /call/batch ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/attendance/call/batch")
    class RegisterBatch {

        @Test
        @DisplayName("retorna 204 quando presenças salvas com sucesso")
        void retorna204QuandoSalvoComSucesso() throws Exception {
            doNothing().when(attendanceService).registerBatch(any());

            var body = new BatchAttendanceRequest(
                    1L, LocalDate.now(), "Tópico",
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            mockMvc.perform(post("/api/v1/attendance/call/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("retorna 422 quando chamada já está fechada")
        void retorna422QuandoChamadaFechada() throws Exception {
            doThrow(new BusinessRuleException("Esta chamada já foi fechada e não pode ser alterada"))
                    .when(attendanceService).registerBatch(any());

            var body = new BatchAttendanceRequest(
                    1L, LocalDate.now(), null,
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            mockMvc.perform(post("/api/v1/attendance/call/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("retorna 400 quando body não tem schoolroomId")
        void retorna400QuandoBodyInvalido() throws Exception {
            var bodyInvalido = """
                    {
                        "date": "%s",
                        "attendances": [{"studentId": 100, "status": "PRESENT"}]
                    }
                    """.formatted(LocalDate.now());

            mockMvc.perform(post("/api/v1/attendance/call/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyInvalido)
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 400 quando attendances está vazio")
        void retorna400QuandoAttendancesVazio() throws Exception {
            var body = new BatchAttendanceRequest(1L, LocalDate.now(), null, List.of());

            mockMvc.perform(post("/api/v1/attendance/call/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 403 quando professor tenta registrar em turma que não é sua")
        void retorna403QuandoProfessorSemPermissao() throws Exception {
            doThrow(new ForbiddenException("Sem permissão"))
                    .when(attendanceService).registerBatch(any());

            var body = new BatchAttendanceRequest(
                    1L, LocalDate.now(), null,
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            mockMvc.perform(post("/api/v1/attendance/call/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(user("outro@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PATCH /call/{id}/close ──────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/attendance/call/{lectureId}/close")
    class CloseCall {

        @Test
        @DisplayName("retorna 204 quando chamada fechada com sucesso")
        void retorna204QuandoFechadaComSucesso() throws Exception {
            doNothing().when(attendanceService).closeCall(10L);

            mockMvc.perform(patch("/api/v1/attendance/call/10/close")
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("retorna 422 quando chamada já está fechada")
        void retorna422QuandoJaFechada() throws Exception {
            doThrow(new BusinessRuleException("Esta chamada já está fechada"))
                    .when(attendanceService).closeCall(10L);

            mockMvc.perform(patch("/api/v1/attendance/call/10/close")
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Esta chamada já está fechada"));
        }

        @Test
        @DisplayName("retorna 404 quando lecture não existe")
        void retorna404QuandoLectureInexistente() throws Exception {
            doThrow(new ResourceNotFoundException("Lecture", 99L))
                    .when(attendanceService).closeCall(99L);

            mockMvc.perform(patch("/api/v1/attendance/call/99/close")
                            .with(user("prof@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /call/{id}/reopen ─────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/attendance/call/{lectureId}/reopen")
    class ReopenCall {

        @Test
        @DisplayName("retorna 204 quando chamada reaberta com sucesso por admin")
        void retorna204QuandoReabertaComSucesso() throws Exception {
            doNothing().when(attendanceService).reopenCall(10L);

            mockMvc.perform(patch("/api/v1/attendance/call/10/reopen")
                            .with(user("admin@sing.local").roles("ADMIN")))
                    .andExpect(status().isNoContent());
        }

        @Test
        void retorna403QuandoProfessorTentaReabrir() throws Exception {
            doThrow(new ForbiddenException("Sem permissão"))
                    .when(attendanceService).reopenCall(10L);

            mockMvc.perform(patch("/api/v1/attendance/call/10/reopen")
                            .with(user("prof1@sing.local").roles("PROFESSOR")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("retorna 422 quando chamada já está aberta")
        void retorna422QuandoJaAberta() throws Exception {
            doThrow(new BusinessRuleException("A aula ainda está aberta"))
                    .when(attendanceService).reopenCall(10L);

            mockMvc.perform(patch("/api/v1/attendance/call/10/reopen")
                            .with(user("admin@sing.local").roles("ADMIN")))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}