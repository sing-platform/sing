package br.cadi.people.api.controller;

import br.cadi.academic.api.dto.response.SchoolRoomResponse;
import br.cadi.academic.application.AcademicAuthorizationService;
import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.academic.application.SchoolRoomService;
import br.cadi.exception.BusinessException;
import br.cadi.test.StandaloneMockMvcSupport;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.enums.CollaboratorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherControllerSchoolRoomsWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private SchoolRoomService schoolRoomService;
    @Mock
    private EnrollmentQueryService enrollmentQueryService;
    @Mock
    private AcademicAuthorizationService academicAuthorizationService;

    @BeforeEach
    void setUp() {
        Mockito.reset(schoolRoomService, enrollmentQueryService, academicAuthorizationService);
        TeacherController controller = new TeacherController(
                schoolRoomService, enrollmentQueryService, academicAuthorizationService
        );
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(controller);
    }

    @Test
    void pageMySchoolRoomsRetorna200() throws Exception {
        Collaborator collaborator = Collaborator.builder()
                .id(10L).name("Prof").email("prof@cadi.com").cpf("111").phone("1")
                .birthDate(LocalDate.of(1980, 1, 1)).educationLevel("sup")
                .type(CollaboratorType.TEACHER).build();
        var page = new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1);
        when(academicAuthorizationService.getCollaboratorOrThrow(any())).thenReturn(collaborator);
        when(schoolRoomService.pageSchoolRoomsForTeacher(eq(10L), eq(true), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/teachers/me/schoolrooms")
                        .with(user("prof@test.com").roles("PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Turma A"));
    }

    @Test
    void pageSchoolRoomsByTeacherRetorna200ParaGestao() throws Exception {
        var page = new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1);
        when(schoolRoomService.pageSchoolRoomsForTeacher(eq(10L), eq(true), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/teachers/10/schoolrooms")
                        .with(user("sec@test.com").roles("SECRETARY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teacherId").value(10L));
    }

    @Test
    void pageSchoolRoomsByTeacherRetorna403QuandoServiceNegaAcesso() throws Exception {
        when(schoolRoomService.pageSchoolRoomsForTeacher(eq(10L), eq(true), any(), any()))
                .thenThrow(new BusinessException("Acesso negado", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/v1/teachers/10/schoolrooms")
                        .with(user("prof@test.com").roles("PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    private SchoolRoomResponse response() {
        return new SchoolRoomResponse(1L, "Turma A", "Seg 08:00–10:00", List.of(),
                "Sala 1", 30, true, 1L, "Oficina", 10L, "Prof", 0, 30);
    }
}
