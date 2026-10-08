package br.sing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para o módulo de comunicação.
 *
 * <p>Topologia:
 * <pre>
 *   Exchange: sing.notifications  (TopicExchange)
 *   │
 *   ├─ email.queue       ← routing key: email.send
 *   ├─ notification.queue← routing key: notification.send
 *   └─ whatsapp.queue    ← routing key: whatsapp.send
 * </pre>
 */
@Configuration
public class RabbitMQConfig {

    // -----------------------------------------------------------------------
    // Nomes de exchanges / queues / routing-keys
    // -----------------------------------------------------------------------

    public static final String EXCHANGE      = "sing.notifications";
    public static final String EMAIL_QUEUE   = "email.queue";
    public static final String EMAIL_RK      = "email.send";

    public static final String NOTIF_QUEUE   = "notification.queue";
    public static final String NOTIF_RK      = "notification.send";

    public static final String WPP_QUEUE     = "whatsapp.queue";
    public static final String WPP_RK        = "whatsapp.send";

    // -----------------------------------------------------------------------
    // Exchange
    // -----------------------------------------------------------------------

    @Bean
    public TopicExchange singExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // -----------------------------------------------------------------------
    // Queues
    // -----------------------------------------------------------------------

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIF_QUEUE).build();
    }

    @Bean
    public Queue whatsappQueue() {
        return QueueBuilder.durable(WPP_QUEUE).build();
    }

    // -----------------------------------------------------------------------
    // Bindings
    // -----------------------------------------------------------------------

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange singExchange) {
        return BindingBuilder.bind(emailQueue).to(singExchange).with(EMAIL_RK);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange singExchange) {
        return BindingBuilder.bind(notificationQueue).to(singExchange).with(NOTIF_RK);
    }

    @Bean
    public Binding whatsappBinding(Queue whatsappQueue, TopicExchange singExchange) {
        return BindingBuilder.bind(whatsappQueue).to(singExchange).with(WPP_RK);
    }

    // -----------------------------------------------------------------------
    // Admin resiliente
    // -----------------------------------------------------------------------

    /**
     * RabbitAdmin com {@code ignoreDeclarationExceptions=true}.
     *
     * <p>Sem esse bean, o Spring Boot auto-configura um {@code RabbitAdmin} que, ao receber o
     * {@code ContextRefreshedEvent}, tenta conectar ao broker para declarar exchanges/queues.
     * Se o broker não estiver disponível, a aplicação falha ao subir com
     * {@code AmqpConnectException: Connection refused}.
     *
     * <p>Com {@code ignoreDeclarationExceptions=true} o {@code AmqpConnectException} é capturado
     * internamente e logado em {@code DEBUG} — a aplicação sobe normalmente.
     * Quando a primeira conexão for estabelecida (lazy, no primeiro envio), o
     * {@code ConnectionListener} registrado pelo {@code RabbitAdmin} declara toda a
     * infraestrutura automaticamente.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        /*
         * autoStartup=false → o RabbitAdmin NÃO tenta conectar no ContextRefreshedEvent.
         * A infraestrutura (exchanges/queues/bindings) é declarada de forma lazy pelo
         * ConnectionListener registrado em afterPropertiesSet(), que dispara assim que a
         * primeira conexão real for estabelecida (no ResilientMessagingService.send()).
         * Isso permite que a aplicação suba normalmente sem o broker disponível.
         */
        admin.setAutoStartup(false);
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }

    // -----------------------------------------------------------------------
    // Serialização JSON
    // -----------------------------------------------------------------------

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
