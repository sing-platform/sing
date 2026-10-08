package br.sing.academic.api.controller;

import br.sing.academic.api.dto.request.EnrollmentRequest;
import br.sing.academic.api.dto.response.EnrollmentPreviewResponse;
import br.sing.academic.api.dto.response.EnrollmentSuggestedAction;
import br.sing.academic.application.EnrollmentService;
import br.sing.test.StandaloneMockMvcSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MockMvc standalone, mesmo padrão de {@link br.sing.academic.api.SchoolRoomControllerCrudWebMvcTest}. */
@ExtendWith(MockitoExtension.class)
class EnrollmentControllerWebMvcTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = StandaloneMockMvcSupport.objectMapper();

    @Mock
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(new EnrollmentController(enrollmentService));
    }

    @Test
    void postMatriculaUmaTurmaRetorna201() throws Exception {
        doNothing().when(enrollmentService).enrollStudent(any(EnrollmentRequest.class));

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(1L, List.of(10L)))))
                .andExpect(status().isCreated())
                .andExpect(content().json("\"Aluno matriculado com sucesso!\""));

        verify(enrollmentService).enrollStudent(any(EnrollmentRequest.class));
    }

    @Test
    void postMatriculaDuasTurmasRetorna201() throws Exception {
        doNothing().when(enrollmentService).enrollStudent(any(EnrollmentRequest.class));

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(1L, List.of(10L, 11L)))))
                .andExpect(status().isCreated())
                .andExpect(content().json("\"Matrículas concluídas com sucesso!\""));

        verify(enrollmentService).enrollStudent(any(EnrollmentRequest.class));
    }

    @Test
    void postPreviewRetorna200ComCorpo() throws Exception {
        when(enrollmentService.previewEnrollment(any(EnrollmentRequest.class)))
                .thenReturn(new EnrollmentPreviewResponse(true, List.of(), EnrollmentSuggestedAction.TRANSFER));

        mockMvc.perform(post("/api/v1/enrollments/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(1L, List.of(10L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.suggestedAction").value("TRANSFER"));

        verify(enrollmentService).previewEnrollment(any(EnrollmentRequest.class));
    }

    @Test
    void deleteUnenrollRetorna204() throws Exception {
        doNothing().when(enrollmentService).unenrollStudent(5L, 9L);

        mockMvc.perform(delete("/api/v1/enrollments/student/5/room/9"))
                .andExpect(status().isNoContent());

        verify(enrollmentService).unenrollStudent(5L, 9L);
    }
}
