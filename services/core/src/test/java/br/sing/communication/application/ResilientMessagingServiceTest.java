package br.sing.communication.application;

import br.sing.communication.domain.EmailMessageEvent;
import br.sing.communication.domain.MessageOutbox;
import br.sing.communication.infra.repositories.MessageOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link ResilientMessagingService}.
 *
 * <p>Comportamento esperado:
 * <ul>
 *   <li>Publica direto quando o broker responde.</li>
 *   <li>Persiste no outbox quando {@code RabbitTemplate} lança {@link AmqpException}.</li>
 *   <li>Reprocessa pendentes em {@code processOutbox()} marcando como processados.</li>
 *   <li>Não interrompe a fila quando uma mensagem específica falha.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ResilientMessagingServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private MessageOutboxRepository outboxRepository;

    private ResilientMessagingService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        service = new ResilientMessagingService(rabbitTemplate, outboxRepository, objectMapper);
    }

    // ------------------------------------------------------------------
    // send()
    // ------------------------------------------------------------------

    @Test
    void sendDevePublicarDiretoQuandoBrokerDisponivel() {
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to("a@sing.local").subject("oi").body("<p>oi</p>").html(true).build();

        service.send("sing.notifications", "email.send", event);

        verify(rabbitTemplate).convertAndSend("sing.notifications", "email.send", event);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void sendDevePersistirNoOutboxQuandoBrokerLanca() {
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to("a@sing.local").subject("oi").body("<p>oi</p>").html(true).build();

        doThrow(new AmqpException("broker offline"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        service.send("sing.notifications", "email.send", event);

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(outboxRepository).save(captor.capture());

        MessageOutbox saved = captor.getValue();
        assertThat(saved.getExchange()).isEqualTo("sing.notifications");
        assertThat(saved.getRoutingKey()).isEqualTo("email.send");
        assertThat(saved.isProcessed()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPayload()).contains("a@sing.local").contains("oi");
    }

    // ------------------------------------------------------------------
    // processOutbox()
    // ------------------------------------------------------------------

    @Test
    void processOutboxDeveReenviarMensagensPendentesEMarcarProcessadas() throws Exception {
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to("a@sing.local").subject("oi").body("<p>oi</p>").html(true).build();
        String payload = objectMapper.writeValueAsString(event);

        MessageOutbox pending = MessageOutbox.builder()
                .id(10L)
                .exchange("sing.notifications")
                .routingKey("email.send")
                .payload(payload)
                .processed(false)
                .build();

        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(pending));

        service.processOutbox();

        verify(rabbitTemplate).convertAndSend(eq("sing.notifications"), eq("email.send"),
                any(EmailMessageEvent.class));

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(outboxRepository).save(captor.capture());
        MessageOutbox saved = captor.getValue();
        assertThat(saved.isProcessed()).isTrue();
        assertThat(saved.getProcessedAt()).isNotNull();
    }

    @Test
    void processOutboxNaoFazNadaQuandoNaoHaPendentes() {
        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of());

        service.processOutbox();

        verifyNoInteractions(rabbitTemplate);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processOutboxDeveContinuarComProximaQuandoUmaFalha() throws Exception {
        EmailMessageEvent ok = EmailMessageEvent.builder()
                .to("a@sing.local").subject("ok").body("ok").html(true).build();
        EmailMessageEvent boom = EmailMessageEvent.builder()
                .to("b@sing.local").subject("boom").body("boom").html(true).build();

        MessageOutbox first = MessageOutbox.builder()
                .id(1L).exchange("ex").routingKey("email.send")
                .payload(objectMapper.writeValueAsString(boom)).processed(false).build();
        MessageOutbox second = MessageOutbox.builder()
                .id(2L).exchange("ex").routingKey("email.send")
                .payload(objectMapper.writeValueAsString(ok)).processed(false).build();

        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(first, second));

        // first lança, second sucesso
        doThrow(new AmqpException("falha temporaria"))
                .doNothing()
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        service.processOutbox();

        // a primeira NÃO é marcada como processada (save apenas para a segunda)
        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2L);
        assertThat(captor.getValue().isProcessed()).isTrue();
    }

    @Test
    void processOutboxUsaObjectComoFallbackQuandoRoutingKeyDesconhecida() throws Exception {
        // payload é string JSON simples; classe alvo cai em Object
        MessageOutbox pending = MessageOutbox.builder()
                .id(1L)
                .exchange("ex")
                .routingKey("rota.qualquer")
                .payload("\"texto livre\"")
                .processed(false)
                .build();

        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(pending));

        service.processOutbox();

        verify(rabbitTemplate).convertAndSend(eq("ex"), eq("rota.qualquer"), any(Object.class));
    }
}
