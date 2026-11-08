package br.sing.reports.infra.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class FinanceClient extends RemoteServiceClient {

    private static final String DEFAULT_URL = "http://localhost:8082";

    @Autowired
    public FinanceClient(
            RestClient.Builder builder,
            @Value("${sing.services.finance.base-url:" + DEFAULT_URL + "}") String baseUrl) {
        this(buildRestClient(builder, baseUrl, DEFAULT_URL));
    }

    FinanceClient(RestClient restClient) {
        super(restClient, "Serviço financeiro");
    }

    public List<Entry> listEntries(String month) {
        List<Entry> entries = get(uri -> {
            uri.path("/api/v1/finance/entries");
            if (month != null && !month.isBlank()) {
                uri.queryParam("month", month);
            }
            return uri.build();
        }, new ParameterizedTypeReference<List<Entry>>() {});
        return entries == null ? List.of() : entries;
    }

    public List<Sponsor> listSponsors() {
        List<Sponsor> sponsors = get(
                uri -> uri.path("/api/v1/finance/sponsors").build(),
                new ParameterizedTypeReference<List<Sponsor>>() {});
        return sponsors == null ? List.of() : sponsors;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            Long id,
            String direction,
            String status,
            LocalDate dueDate,
            BigDecimal amount,
            String category,
            String description,
            Long sponsorId,
            String sponsorName,
            Boolean active
    ) {
        public boolean ativo() {
            return !Boolean.FALSE.equals(active);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sponsor(
            Long id,
            Long studentId,
            String name,
            BigDecimal amount,
            String frequency,
            LocalDate startDate,
            LocalDate lastGeneratedDate,
            Boolean active
    ) {
        public boolean ativo() {
            return !Boolean.FALSE.equals(active);
        }
    }
}
