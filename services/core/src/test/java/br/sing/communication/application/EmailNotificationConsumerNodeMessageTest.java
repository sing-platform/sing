package br.sing.communication.application;

import br.sing.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailNotificationConsumerNodeMessageTest {

    private EmailService emailService;
    private ChannelAwareMessageListener listener;

    @BeforeEach
    void setUp() throws Exception {
        emailService = mock(EmailService.class);
        EmailNotificationConsumer consumer = new EmailNotificationConsumer(emailService);

        Method method = EmailNotificationConsumer.class.getMethod(
                "handleEmailEvent", br.sing.communication.domain.EmailMessageEvent.class);
        DefaultMessageHandlerMethodFactory handlerFactory = new DefaultMessageHandlerMethodFactory();
        handlerFactory.afterPropertiesSet();

        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        endpoint.setId("email-consumer-test");
        endpoint.setBean(consumer);
        endpoint.setMethod(method);
        endpoint.setMessageHandlerMethodFactory(handlerFactory);
        endpoint.setQueueNames(RabbitMQConfig.EMAIL_QUEUE);

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(mock(ConnectionFactory.class));
        factory.setMessageConverter(new RabbitMQConfig().jsonMessageConverter());

        MessageListenerContainer container = factory.createListenerContainer(endpoint);
        listener = (ChannelAwareMessageListener) container.getMessageListener();
    }

    private static Message jsonMessage(String json, String contentType) {
        MessageProperties props = new MessageProperties();
        props.setContentType(contentType);
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void aceitaJsonPublicadoPeloNodeSemHeaderTypeId() throws Exception {
        Message message = jsonMessage(
                "{\"to\":\"aluno@sing.local\",\"subject\":\"Nova publicação no mural: Aviso\","
                        + "\"body\":\"<html>oi</html>\",\"html\":true}",
                "application/json");

        listener.onMessage(message, null);

        verify(emailService).sendHtmlEmail(
                "aluno@sing.local", "Nova publicação no mural: Aviso", "<html>oi</html>", true);
    }

    @Test
    void aceitaJsonComCharsetNoContentType() throws Exception {
        Message message = jsonMessage(
                "{\"to\":\"a@sing.local\",\"subject\":\"s\",\"body\":\"texto\",\"html\":false}",
                "application/json; charset=utf-8");

        listener.onMessage(message, null);

        verify(emailService).sendHtmlEmail("a@sing.local", "s", "texto", false);
    }
}
