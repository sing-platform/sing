package br.sing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Inicializador resiliente dos {@code @RabbitListener}.
 *
 * <p>Como {@code spring.rabbitmq.listener.simple.auto-startup=false}, os listeners
 * não tentam conectar ao broker durante o boot — a aplicação sobe normalmente
 * mesmo sem RabbitMQ. Este scheduler verifica periodicamente o registro de
 * endpoints e tenta iniciá-los: assim, quando o broker voltar, os consumidores
 * passam a consumir sem necessidade de reiniciar a aplicação.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitListenerInitializer {

    private final RabbitListenerEndpointRegistry registry;

    @Scheduled(fixedDelay = 300_000) // a cada 5 minutos
    public void checkAndStartListeners() {
        if (registry.isRunning()) return;
        try {
            log.info("Tentando iniciar listeners RabbitMQ...");
            registry.start();
            if (registry.isRunning()) {
                log.info("Listeners RabbitMQ iniciados com sucesso.");
            }
        } catch (Exception e) {
            log.warn("Não foi possível iniciar listeners RabbitMQ — broker pode estar indisponível: {}",
                    e.getMessage());
        }
    }
}