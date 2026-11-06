package br.sing.academic.application;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Publica o envelope já gravado. As duas filas são declaradas aqui para a mensagem
 * não se perder se o consumidor ainda não subiu. Cada consumidor também declara a sua.
 */
@Service
public class RabbitMatriculaEventoPublisher implements MatriculaEventoPublisher {

    public static final String ROUTING_KEY = "matricula.criada";
    public static final String FILA_DOCUMENTS = "documents.matricula.criada";
    public static final String FILA_COMMUNICATION = "communication.matricula.criada";

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitMatriculaEventoPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${sing.events.exchange:sing.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publicar(String envelopeJson) {
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare(exchange, "topic", true);
            channel.queueDeclare(FILA_DOCUMENTS, true, false, false, null);
            channel.queueBind(FILA_DOCUMENTS, exchange, ROUTING_KEY);
            channel.queueDeclare(FILA_COMMUNICATION, true, false, false, null);
            channel.queueBind(FILA_COMMUNICATION, exchange, ROUTING_KEY);
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();
            channel.basicPublish(
                    exchange,
                    ROUTING_KEY,
                    props,
                    envelopeJson.getBytes(StandardCharsets.UTF_8));
            return null;
        });
    }
}
