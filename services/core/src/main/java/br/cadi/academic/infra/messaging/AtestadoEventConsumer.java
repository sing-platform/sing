package br.cadi.academic.infra.messaging;

import br.cadi.academic.application.AtestadoJustificationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Consome as decisoes sobre atestados publicadas pelo servico de documentos.
 *
 * <p>Justificar falta tolera consistencia eventual: a secretaria aprova o
 * atestado e o efeito no livro de chamada pode chegar segundos depois. O que
 * nao tolera — matricula, vaga, chamada, declaracao — continua sincrono e
 * dentro do core.</p>
 *
 * <p><strong>Idempotencia:</strong> os dois handlers podem ser reexecutados sem
 * efeito colateral. RabbitMQ garante entrega <em>at-least-once</em>, entao
 * mensagem repetida e esperada, nao excecao.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AtestadoEventConsumer {

    private final AtestadoJustificationService justificationService;

    @RabbitListener(queues = EventsRabbitConfig.ATESTADO_QUEUE)
    public void onAtestadoEvent(JsonNode envelope) {
        String eventType = envelope.path("eventType").asText("");
        JsonNode payload = envelope.path("payload");

        switch (eventType) {
            case "AtestadoApproved" -> handleApproved(payload);
            case "AtestadoRevoked" -> handleRevoked(payload);
            default -> log.warn("Evento de atestado desconhecido: {}", eventType);
        }
    }

    private void handleApproved(JsonNode payload) {
        Long documentId = payload.path("documentId").asLong();
        Long ownerPersonId = payload.path("ownerPersonId").asLong();
        String start = payload.path("referenceDateStart").asText(null);
        String end = payload.path("referenceDateEnd").asText(null);

        if (documentId == 0 || ownerPersonId == 0 || start == null || end == null) {
            log.warn("AtestadoApproved incompleto, ignorado: {}", payload);
            return;
        }
        justificationService.applyJustifications(
                documentId, ownerPersonId, LocalDate.parse(start), LocalDate.parse(end));
    }

    private void handleRevoked(JsonNode payload) {
        Long documentId = payload.path("documentId").asLong();
        if (documentId == 0) {
            log.warn("AtestadoRevoked sem documentId, ignorado");
            return;
        }
        justificationService.revertJustifications(documentId);
    }
}
