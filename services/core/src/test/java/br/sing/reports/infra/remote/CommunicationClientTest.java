package br.sing.reports.infra.remote;

import br.sing.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CommunicationClientTest {

    private MockRestServiceServer server;
    private CommunicationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://communication");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CommunicationClient(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void buscaEngajamentoComPeriodoEBearer() {
        server.expect(requestTo("http://communication/api/v1/mural/engajamento?start=2026-11-01&end=2026-11-30"))
                .andExpect(queryParam("start", "2026-11-01"))
                .andExpect(queryParam("end", "2026-11-30"))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("""
                        [{"postId": 4, "title": "Aviso", "createdAt": "2026-11-05T10:30:00",
                          "authorUsername": "admin", "viewCount": 12, "readCount": 7}]
                        """, MediaType.APPLICATION_JSON));

        List<CommunicationClient.MuralEngagement> rows =
                client.muralEngagement(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(rows).containsExactly(
                new CommunicationClient.MuralEngagement(4L, "Aviso", "2026-11-05T10:30:00", "admin", 12L, 7L));
        server.verify();
    }

    @Test
    void propaga403() {
        server.expect(requestTo("http://communication/api/v1/mural/engajamento?start=2026-11-01&end=2026-11-30"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.muralEngagement(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void foraDoArVirraServiceUnavailable() {
        server.expect(requestTo("http://communication/api/v1/mural/engajamento?start=2026-11-01&end=2026-11-30"))
                .andRespond(request -> {
                    throw new IOException("timeout");
                });

        assertThatThrownBy(() -> client.muralEngagement(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void erro500VirraBadGateway() {
        server.expect(requestTo("http://communication/api/v1/mural/engajamento?start=2026-11-01&end=2026-11-30"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.muralEngagement(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
