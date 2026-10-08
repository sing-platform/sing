package br.sing.communication.application;

import br.sing.communication.domain.EmailMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor da fila {@code email.queue}.
 *
 * <p>Escuta eventos do tipo {@link EmailMessageEvent} publicados com a
 * routing key {@code email.send} no exchange {@code sing.notifications}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "#{T(br.sing.config.RabbitMQConfig).EMAIL_QUEUE}")
    public void handleEmailEvent(EmailMessageEvent event) {
        log.info("Recebendo e-mail para: {}", event.getTo());
        try {
            emailService.sendHtmlEmail(
                    event.getTo(),
                    event.getSubject(),
                    event.getBody(),
                    event.isHtml()
            );
        } catch (Exception ex) {
            log.error("Falha ao processar e-mail para {}: {}", event.getTo(), ex.getMessage());
            // Re-throw para que o RabbitMQ possa fazer retry / DLQ no futuro
            throw ex;
        }
    }
}
