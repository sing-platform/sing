package br.sing.academic.api.controller;

import br.sing.academic.api.dto.request.EnrollmentRequest;
import br.sing.academic.api.dto.response.EnrollmentPreviewResponse;
import br.sing.academic.api.dto.response.EnrollmentSuggestedAction;
import br.sing.academic.api.dto.response.IntegracaoMatriculaResponse;
import br.sing.academic.api.dto.response.MatriculaItemResponse;
import br.sing.academic.api.dto.response.MatriculasCriadasResponse;
import br.sing.academic.application.EnrollmentService;
import br.sing.academic.application.MatriculaIntegracaoService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MockMvc standalone, mesmo padrão de {@link br.sing.academic.api.SchoolRoomControllerCrudWebMvcTest}. */
@ExtendWith(MockitoExtension.class)
class EnrollmentControllerWebMvcTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = StandaloneMockMvcSupport.objectMapper();

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private MatriculaIntegracaoService matriculaIntegracaoService;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(
                new EnrollmentController(enrollmentService, matriculaIntegracaoService));
    }

    @Test
    void postMatriculaUmaTurmaRetorna201() throws Exception {
        when(enrollmentService.enrollStudent(any(EnrollmentRequest.class)))
                .thenReturn(new MatriculasCriadasResponse(List.of(
                        new MatriculaItemResponse(9001L, "11111111-1111-4111-8111-111111111111", "pendente", "pendente")
                )));

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(1L, List.of(10L)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matriculas[0].matriculaId").value(9001))
                .andExpect(jsonPath("$.matriculas[0].eventId").value("11111111-1111-4111-8111-111111111111"))
                .andExpect(jsonPath("$.matriculas[0].publicacao").value("pendente"))
                .andExpect(jsonPath("$.matriculas[0].cobranca").value("pendente"));

        verify(enrollmentService).enrollStudent(any(EnrollmentRequest.class));
    }

    @Test
    void postMatriculaDuasTurmasRetorna201() throws Exception {
        when(enrollmentService.enrollStudent(any(EnrollmentRequest.class)))
                .thenReturn(new MatriculasCriadasResponse(List.of(
                        new MatriculaItemResponse(9001L, "11111111-1111-4111-8111-111111111111", "pendente", "pendente"),
                        new MatriculaItemResponse(9002L, "22222222-2222-4222-8222-222222222222", "pendente", "pendente")
                )));

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentRequest(1L, List.of(10L, 11L)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matriculas.length()").value(2))
                .andExpect(jsonPath("$.matriculas[1].matriculaId").value(9002));

        verify(enrollmentService).enrollStudent(any(EnrollmentRequest.class));
    }

    @Test
    void getIntegracoesRetornaAndamento() throws Exception {
        when(matriculaIntegracaoService.consultar(9001L))
                .thenReturn(new IntegracaoMatriculaResponse(
                        9001L, "11111111-1111-4111-8111-111111111111", "pendente", "pendente", 0, 0));

        mockMvc.perform(get("/api/v1/enrollments/9001/integracoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matriculaId").value(9001))
                .andExpect(jsonPath("$.tentativasPublicacao").value(0))
                .andExpect(jsonPath("$.tentativasCobranca").value(0));
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
