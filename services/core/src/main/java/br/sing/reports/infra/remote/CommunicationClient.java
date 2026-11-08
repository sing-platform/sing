package br.sing.reports.infra.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
public class CommunicationClient extends RemoteServiceClient {

    private static final String DEFAULT_URL = "http://localhost:3000";

    @Autowired
    public CommunicationClient(
            RestClient.Builder builder,
            @Value("${sing.services.communication.base-url:" + DEFAULT_URL + "}") String baseUrl) {
        this(buildRestClient(builder, baseUrl, DEFAULT_URL));
    }

    CommunicationClient(RestClient restClient) {
        super(restClient, "Serviço de comunicação");
    }

    public List<MuralEngagement> muralEngagement(LocalDate start, LocalDate end) {
        List<MuralEngagement> rows = get(
                uri -> uri.path("/api/v1/mural/engajamento")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .build(),
                new ParameterizedTypeReference<List<MuralEngagement>>() {});
        return rows == null ? List.of() : rows;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MuralEngagement(
            Long postId,
            String title,
            String createdAt,
            String authorUsername,
            Long viewCount,
            Long readCount
    ) {}
}
