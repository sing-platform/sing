package br.sing.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstanciaControllerTest {

    @Test
    void devolveONomeDaInstanciaSemCache() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InstanciaController())
                .addFilters(new PodHeaderFilter())
                .build();

        mockMvc.perform(get("/api/v1/instancia").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Pod", PodHeaderFilter.nomeInstancia()))
                .andExpect(jsonPath("$.servico").value("core"))
                .andExpect(jsonPath("$.instancia").value(PodHeaderFilter.nomeInstancia()));
    }
}
