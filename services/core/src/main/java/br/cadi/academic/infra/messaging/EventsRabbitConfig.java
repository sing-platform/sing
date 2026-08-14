package br.cadi.academic.infra.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange de eventos de dominio entre servicos.
 *
 * <p>Separada da exchange {@code cadi.notifications} (e-mail/notificacao), que
 * trata de entrega ao usuario. Aqui trafega fato de negocio entre servicos.</p>
 */
@Configuration
public class EventsRabbitConfig {

    public static final String EVENTS_EXCHANGE = "cadi.events";
    public static final String ATESTADO_QUEUE = "core.atestado.events";
    public static final String ATESTADO_DLQ = "core.atestado.events.dlq";

    @Value("${cadi.events.exchange:" + EVENTS_EXCHANGE + "}")
    private String exchangeName;

    @Bean
    public TopicExchange cadiEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * Fila com dead-letter: mensagem que falha repetidamente sai da fila
     * principal em vez de bloquear as seguintes.
     */
    @Bean
    public Queue atestadoQueue() {
        return QueueBuilder.durable(ATESTADO_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(ATESTADO_DLQ)
                .build();
    }

    @Bean
    public Queue atestadoDlq() {
        return QueueBuilder.durable(ATESTADO_DLQ).build();
    }

    @Bean
    public Binding atestadoBinding(Queue atestadoQueue, TopicExchange cadiEventsExchange) {
        return BindingBuilder.bind(atestadoQueue).to(cadiEventsExchange).with("atestado.*");
    }
}
