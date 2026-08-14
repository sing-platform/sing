package br.cadi.academic.infra.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

/**
 * Cliente do servico de documentos.
 *
 * <p>Substitui a chamada direta a {@code AtestadoApprovalService}, que deixou de
 * existir no core quando o modulo virou servico proprio.</p>
 *
 * <p><strong>Degradacao graciosa:</strong> se o servico estiver fora, a consulta
 * devolve {@code null} e a falta e gravada como ABSENT. Nada se perde: quando o
 * atestado for aprovado, o evento {@code AtestadoApproved} justifica a falta
 * retroativamente. O caminho inverso — bloquear a chamada porque um servico
 * auxiliar caiu — deixaria o professor sem registrar presenca.</p>
 */
@Component
@Slf4j
public class DocumentsClient {

    private final RestClient restClient;

    public DocumentsClient(@Value("${cadi.services.documents.base-url:http://documents:8000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Id do atestado aprovado que cobre a data, ou {@code null} se nao houver
     * (ou se o servico estiver indisponivel).
     */
    public Long findCoveringApprovedAtestado(Long studentPersonId, LocalDate date, String bearerToken) {
        try {
            Map<?, ?> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/documents/internal/covering")
                            .queryParam("person_id", studentPersonId)
                            .queryParam("day", date.toString())
                            .build())
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                return null;
            }
            Object id = body.get("documentId");
            return id == null ? null : Long.valueOf(id.toString());
        } catch (Exception e) {
            log.warn("Servico de documentos indisponivel ao checar atestado (aluno={}, data={}): {}",
                    studentPersonId, date, e.getMessage());
            return null;
        }
    }
}
