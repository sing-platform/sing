package br.sing.academic.api;

import br.sing.academic.api.controller.WorkshopController;
import br.sing.academic.api.dto.request.WorkshopCreateRequest;
import br.sing.academic.api.dto.response.WorkshopResponse;
import br.sing.academic.api.dto.request.WorkshopUpdateRequest;
import br.sing.academic.application.WorkshopService;
import br.sing.exception.BusinessException;
import br.sing.test.StandaloneMockMvcSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkshopControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = StandaloneMockMvcSupport.objectMapper();

    @Mock
    private WorkshopService workshopService;

    @BeforeEach
    void setUp() {
        Mockito.reset(workshopService);
        WorkshopController controller = new WorkshopController(workshopService);
        mockMvc = StandaloneMockMvcSupport.standaloneSetup(controller);
    }

    @Test
    void listAllRetorna200() throws Exception {
        when(workshopService.listAll(true)).thenReturn(List.of(new WorkshopResponse(1L, "Oficina A", true)));

        mockMvc.perform(get("/api/v1/workshops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Oficina A"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void createRetorna201() throws Exception {
        when(workshopService.create(eq(new WorkshopCreateRequest("Oficina A"))))
                .thenReturn(new WorkshopResponse(1L, "Oficina A", true));

        mockMvc.perform(post("/api/v1/workshops")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkshopCreateRequest("Oficina A"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Oficina A"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createRetorna409QuandoServiceLancaConflito() throws Exception {
        when(workshopService.create(eq(new WorkshopCreateRequest("Duplicada"))))
                .thenThrow(new BusinessException("Conflito", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/workshops")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkshopCreateRequest("Duplicada"))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateRetorna200() throws Exception {
        when(workshopService.update(eq(1L), eq(new WorkshopUpdateRequest("Nova Oficina"))))
                .thenReturn(new WorkshopResponse(1L, "Nova Oficina", true));

        mockMvc.perform(put("/api/v1/workshops/1")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkshopUpdateRequest("Nova Oficina"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Nova Oficina"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivateRetorna204() throws Exception {
        mockMvc.perform(delete("/api/v1/workshops/1")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateRetorna409QuandoServiceLancaConflito() throws Exception {
        Mockito.doThrow(new BusinessException("Bloqueado", HttpStatus.CONFLICT))
                .when(workshopService).deactivateWithCascade(1L);

        mockMvc.perform(delete("/api/v1/workshops/1")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isConflict());
    }
}
