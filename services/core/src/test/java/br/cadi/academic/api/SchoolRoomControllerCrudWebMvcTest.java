package br.cadi.academic.api;

import br.cadi.academic.api.controller.SchoolRoomController;
import br.cadi.academic.api.dto.request.ScheduleSlotRequest;
import br.cadi.academic.api.dto.request.SchoolRoomCreateRequest;
import br.cadi.academic.api.dto.request.SchoolRoomUpdateRequest;
import br.cadi.academic.api.dto.response.SchoolRoomResponse;
import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.academic.application.SchoolRoomService;
import br.cadi.common.application.AuditLogService;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.test.StandaloneMockMvcSupport;
import br.cadi.people.api.dto.request.AssignTeacherRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SchoolRoomControllerCrudWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = StandaloneMockMvcSupport.objectMapper();

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
    void createRetorna201() throws Exception {
        var request = new SchoolRoomCreateRequest("Turma A", slots(), "Sala 1", 1L, 30, null);
        when(schoolRoomService.create(eq(request))).thenReturn(response());

        mockMvc.perform(post("/api/v1/schoolrooms")
                        .with(user("sec@test.com").roles("SECRETARY"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Turma A"))
                .andExpect(jsonPath("$.workshopName").value("Oficina"));
    }

    @Test
    void updateRetorna200() throws Exception {
        var request = new SchoolRoomUpdateRequest("Turma A", slots(), "Sala 1", 1L, 30, null, true);
        when(schoolRoomService.update(eq(1L), eq(request))).thenReturn(response());

        mockMvc.perform(put("/api/v1/schoolrooms/1")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Turma A"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivateRetorna204() throws Exception {
        mockMvc.perform(delete("/api/v1/schoolrooms/1")
                        .with(user("sec@test.com").roles("SECRETARY")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateRetorna409QuandoServiceLancaConflito() throws Exception {
        Mockito.doThrow(new BusinessException("Conflito", HttpStatus.CONFLICT))
                .when(schoolRoomService).deactivate(1L);

        mockMvc.perform(delete("/api/v1/schoolrooms/1")
                        .with(user("sec@test.com").roles("SECRETARY")))
                .andExpect(status().isConflict());
    }

    @Test
    void assignTeacherRetorna200() throws Exception {
        var request = new AssignTeacherRequest(10L);
        when(schoolRoomService.assignTeacher(eq(1L), eq(request))).thenReturn(response());

        mockMvc.perform(put("/api/v1/schoolrooms/1/teacher")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(10L));
    }

    @Test
    void assignTeacherRetorna404QuandoProfessorNaoExiste() throws Exception {
        var request = new AssignTeacherRequest(99L);
        when(schoolRoomService.assignTeacher(eq(1L), eq(request)))
                .thenThrow(new EntityNotFoundException("Professor não encontrado"));

        mockMvc.perform(put("/api/v1/schoolrooms/1/teacher")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeTeacherRetorna200() throws Exception {
        when(schoolRoomService.removeTeacher(1L)).thenReturn(response());

        mockMvc.perform(delete("/api/v1/schoolrooms/1/teacher")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void removeTeacherRetorna404QuandoTurmaNaoExiste() throws Exception {
        when(schoolRoomService.removeTeacher(1L)).thenThrow(new EntityNotFoundException("Turma não encontrada"));

        mockMvc.perform(delete("/api/v1/schoolrooms/1/teacher")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    private List<ScheduleSlotRequest> slots() {
        return List.of(new ScheduleSlotRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));
    }

    private SchoolRoomResponse response() {
        return new SchoolRoomResponse(1L, "Turma A", "Seg 08:00–10:00", List.of(),
                "Sala 1", 30, true, 1L, "Oficina", 10L, "Professor", 0, 30);
    }
}
