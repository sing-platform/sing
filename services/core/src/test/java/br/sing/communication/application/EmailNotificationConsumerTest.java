package br.sing.communication.application;

import br.sing.communication.domain.EmailMessageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Testes unitários para {@link EmailNotificationConsumer}.
 *
 * <p>Garante que eventos consumidos da fila {@code email.queue} são delegados
 * ao {@link EmailService} com a flag HTML correta, e que falhas são propagadas
 * para que o RabbitMQ aplique retry/DLQ.
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationConsumerTest {

    @Mock
    private EmailService emailService;

    private EmailNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EmailNotificationConsumer(emailService);
    }

    @Test
    void handleEmailEventDeveDelegarAoEmailServiceComFlagHtmlVerdadeira() {
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to("a@sing.local")
                .subject("Assunto")
                .body("<p>Corpo</p>")
                .html(true)
                .build();

        consumer.handleEmailEvent(event);

        verify(emailService).sendHtmlEmail("a@sing.local", "Assunto", "<p>Corpo</p>", true);
    }

    @Test
    void handleEmailEventDeveDelegarComFlagHtmlFalsa() {
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to("a@sing.local")
                .subject("Assunto")
                .body("texto puro")
                .html(false)
                .build();

        consumer.handleEmailEvent(event);

        verify(emailService).sendHtmlEmail("a@sing.local", "Assunto", "texto puro", false);
    }

    @Test
    void handleEmailEventDeveRepassarExcecaoParaPermitirRetryDoBroker() {
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to("a@sing.local")
                .subject("Assunto")
                .body("Corpo")
                .html(true)
                .build();

        doThrow(new RuntimeException("smtp falhou"))
                .when(emailService).sendHtmlEmail("a@sing.local", "Assunto", "Corpo", true);

        // exceção precisa propagar para que o RabbitMQ aplique retry / DLQ
        assertThatThrownBy(() -> consumer.handleEmailEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("smtp falhou");
    }
}
