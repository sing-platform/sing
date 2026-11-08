package br.sing.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class PodHeaderFilterTest {

    @Test
    void respostaLevaOHostname() throws Exception {
        PodHeaderFilter filter = new PodHeaderFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> { });

        assertThat(response.getHeader("X-Pod")).isNotBlank();
    }
}
