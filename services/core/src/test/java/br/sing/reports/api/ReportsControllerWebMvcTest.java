package br.sing.reports.api;

import br.sing.reports.api.controller.ReportsController;
import br.sing.reports.api.dto.ReportMetadataResponse;
import br.sing.reports.api.dto.ReportResultResponse;
import br.sing.reports.application.ReportExportService;
import br.sing.reports.application.ReportService;
import br.sing.reports.domain.ReportResult;
import br.sing.test.StandaloneMockMvcSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportsControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @Mock
    private ReportExportService exportService;

    @BeforeEach
    void setUp() {
        ReportsController controller = new ReportsController(reportService, exportService);
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(controller);
    }

    @Test
    void listRetorna200() throws Exception {
        when(reportService.listMetadata()).thenReturn(List.of(
                new ReportMetadataResponse("frequencia-por-turma", "Freq", "Desc", "Frequência", List.of())
        ));

        mockMvc.perform(get("/api/v1/reports").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("frequencia-por-turma"));
    }

    @Test
    void runRetorna200() throws Exception {
        when(reportService.run(eq("frequencia-por-turma"), any())).thenReturn(
                new ReportResultResponse(
                        "frequencia-por-turma",
                        "Freq",
                        List.of("Turma"),
                        List.of("schoolroom_name"),
                        List.of(List.of("Turma A")),
                        Map.of(),
                        1
                )
        );

        mockMvc.perform(get("/api/v1/reports/frequencia-por-turma")
                        .param("start", "2025-01-01")
                        .param("end", "2025-12-31")
                        .with(user("sec").roles("SECRETARY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1));
    }
}
