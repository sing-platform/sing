package br.sing.reports.infra.remote;

import br.sing.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FinanceClientTest {

    private static final String ENTRIES = """
            [
              {"id": 10, "direction": "RECEIVABLE", "status": "OVERDUE", "dueDate": "2026-10-05", "paidAt": null,
               "amount": 150.50, "category": "Mensalidade", "description": "Out", "sponsorId": 3,
               "sponsorName": "Padrinho X", "fiscalDocumentId": null, "fiscalDocumentNumber": null, "active": true},
              {"id": 11, "direction": "PAYABLE", "status": "PAID", "dueDate": "2026-10-07", "paidAt": "2026-10-07",
               "amount": 40, "category": "Material", "description": null, "sponsorId": null,
               "sponsorName": null, "fiscalDocumentId": 1, "fiscalDocumentNumber": "NF-1", "active": true}
            ]
            """;

    private MockRestServiceServer server;
    private FinanceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://finance");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FinanceClient(builder.build());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void bearer(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader("Authorization", value);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void listaLancamentosRepassandoOBearerEMapeandoOsCampos() {
        bearer("Bearer abc.def.ghi");
        server.expect(requestTo("http://finance/api/v1/finance/entries"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer abc.def.ghi"))
                .andRespond(withSuccess(ENTRIES, MediaType.APPLICATION_JSON));

        List<FinanceClient.Entry> entries = client.listEntries(null);

        assertThat(entries).hasSize(2);
        FinanceClient.Entry first = entries.get(0);
        assertThat(first.id()).isEqualTo(10L);
        assertThat(first.direction()).isEqualTo("RECEIVABLE");
        assertThat(first.status()).isEqualTo("OVERDUE");
        assertThat(first.dueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(first.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(first.sponsorName()).isEqualTo("Padrinho X");
        assertThat(first.ativo()).isTrue();
        server.verify();
    }

    @Test
    void enviaMesComoQueryParam() {
        bearer("Bearer t");
        server.expect(requestTo("http://finance/api/v1/finance/entries?month=2026-10"))
                .andExpect(queryParam("month", "2026-10"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.listEntries("2026-10")).isEmpty();
        server.verify();
    }

    @Test
    void listaPadrinhos() {
        bearer("Bearer t");
        server.expect(requestTo("http://finance/api/v1/finance/sponsors"))
                .andRespond(withSuccess("""
                        [{"id": 1, "studentId": 9, "studentName": null, "name": "Ana", "email": null, "phone": null,
                          "amount": 100, "frequency": "MONTHLY", "startDate": "2026-01-15",
                          "lastGeneratedDate": "2026-10-15", "active": false}]
                        """, MediaType.APPLICATION_JSON));

        List<FinanceClient.Sponsor> sponsors = client.listSponsors();

        assertThat(sponsors).hasSize(1);
        assertThat(sponsors.get(0).studentId()).isEqualTo(9L);
        assertThat(sponsors.get(0).frequency()).isEqualTo("MONTHLY");
        assertThat(sponsors.get(0).lastGeneratedDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(sponsors.get(0).ativo()).isFalse();
    }

    @Test
    void naoEnviaAuthorizationQuandoRequisicaoNaoTemToken() {
        bearer(null);
        server.expect(requestTo("http://finance/api/v1/finance/sponsors"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.listSponsors();

        server.verify();
    }

    @Test
    void propaga401() {
        bearer("Bearer t");
        server.expect(requestTo("http://finance/api/v1/finance/entries")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.listEntries(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void propaga403() {
        bearer("Bearer t");
        server.expect(requestTo("http://finance/api/v1/finance/entries")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.listEntries(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void erro500VirraBadGateway() {
        bearer("Bearer t");
        server.expect(requestTo("http://finance/api/v1/finance/entries"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.listEntries(null))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getMessage()).contains("Serviço financeiro").contains("500");
                });
    }

    @Test
    void servicoForaDoArVirraServiceUnavailable() {
        bearer("Bearer t");
        server.expect(requestTo("http://finance/api/v1/finance/entries"))
                .andRespond(request -> {
                    throw new IOException("connection refused");
                });

        assertThatThrownBy(() -> client.listEntries(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void corpoInvalidoVirraBadGateway() {
        bearer("Bearer t");
        server.expect(requestTo("http://finance/api/v1/finance/entries"))
                .andRespond(withSuccess("{\"nao\":\"lista\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.listEntries(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
