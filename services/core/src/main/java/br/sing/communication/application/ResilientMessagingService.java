package br.sing.communication.application;

import br.sing.communication.domain.MessageOutbox;
import br.sing.communication.infra.repositories.MessageOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Envio resiliente de mensagens ao RabbitMQ com fallback via Outbox.
 *
 * <p>Fluxo principal:
 * <ol>
 *   <li>Tenta enviar diretamente ao broker.</li>
 *   <li>Se o broker estiver indisponível, persiste a mensagem na tabela
 *       {@code message_outbox} como JSON.</li>
 *   <li>Um scheduler reprocessa as mensagens pendentes a cada 60 segundos.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientMessagingService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Envia uma mensagem ao RabbitMQ.
     * Se o envio falhar, persiste no outbox para retentativa posterior.
     *
     * @param exchange   nome do exchange
     * @param routingKey routing key
     * @param event      objeto a ser serializado como payload
     */
    public void send(String exchange, String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.debug("Mensagem publicada → {}/{}", exchange, routingKey);
        } catch (Exception ex) {
            log.warn("RabbitMQ indisponível ({}) — salvando no outbox: {}/{}", ex.getMessage(), exchange, routingKey);
            saveToOutbox(exchange, routingKey, event);
        }
    }

    // -----------------------------------------------------------------------
    // Scheduler de reprocessamento
    // -----------------------------------------------------------------------

    /**
     * Reprocessa mensagens pendentes no outbox a cada 60 segundos.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processOutbox() {
        List<MessageOutbox> pending = outboxRepository.findByProcessedFalse();
        if (pending.isEmpty()) return;

        log.info("Outbox: {} mensagem(ns) pendente(s)", pending.size());

        for (MessageOutbox msg : pending) {
            try {
                Object event = deserializePayload(msg.getRoutingKey(), msg.getPayload());
                rabbitTemplate.convertAndSend(msg.getExchange(), msg.getRoutingKey(), event);

                msg.setProcessed(true);
                msg.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(msg);
                log.info("Outbox: mensagem {} reenviada com sucesso", msg.getId());
            } catch (Exception ex) {
                log.warn("Outbox: falha ao reenviar mensagem {}: {}", msg.getId(), ex.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void saveToOutbox(String exchange, String routingKey, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            MessageOutbox outbox = MessageOutbox.builder()
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(payload)
                    .processed(false)
                    .build();
            if (outbox.getCreatedAt() == null) {
                outbox.setCreatedAt(LocalDateTime.now());
            }
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Falha crítica ao salvar mensagem no outbox: {}", e.getMessage(), e);
        }
    }

    private Object deserializePayload(String routingKey, String payload) throws Exception {
        Class<?> targetClass = resolveTargetClass(routingKey);
        return objectMapper.readValue(payload, targetClass);
    }

    /**
     * Determina a classe de destino pela routing key.
     * Adicione novos casos conforme novos eventos forem criados.
     */
    private Class<?> resolveTargetClass(String routingKey) {
        return switch (routingKey) {
            case "email.send" -> br.sing.communication.domain.EmailMessageEvent.class;
            default           -> Object.class;
        };
    }
}
