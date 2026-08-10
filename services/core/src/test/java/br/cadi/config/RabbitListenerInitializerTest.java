package br.cadi.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;

import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link RabbitListenerInitializer}.
 *
 * <p>Cobre:
 * <ul>
 *   <li>Quando o registry já está rodando, nada é feito.</li>
 *   <li>Quando está parado, tenta iniciar.</li>
 *   <li>Quando o broker está indisponível, exceções são silenciosamente logadas
 *       e a aplicação não cai.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RabbitListenerInitializerTest {

    @Mock
    private RabbitListenerEndpointRegistry registry;

    private RabbitListenerInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new RabbitListenerInitializer(registry);
    }

    @Test
    void naoFazNadaQuandoRegistryJaEstaRunning() {
        when(registry.isRunning()).thenReturn(true);

        initializer.checkAndStartListeners();

        verify(registry, never()).start();
    }

    @Test
    void tentaIniciarListenersQuandoRegistryParado() {
        when(registry.isRunning())
                .thenReturn(false) // chamada inicial
                .thenReturn(true); // após start()

        initializer.checkAndStartListeners();

        verify(registry).start();
    }

    @Test
    void apenasLogaQuandoStartLancaExcecao() {
        when(registry.isRunning()).thenReturn(false);
        doThrow(new AmqpException("broker offline")).when(registry).start();

        // não deve propagar — método deve apenas logar e retornar
        initializer.checkAndStartListeners();

        verify(registry).start();
    }
}
