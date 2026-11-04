package br.sing.academic.infra.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatriculaEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MatriculaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new MatriculaEventPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", "sing.events");
    }

    @Test
    void publicaJsonComOsCamposDaMatricula() {
        LocalDate data = LocalDate.of(2026, 11, 4);

        publisher.publicarMatriculaCriada(10L, 20L, 30L, data);

        verify(rabbitTemplate).convertAndSend(
                eq("sing.events"),
                eq("matricula.criada"),
                eq(Map.of(
                        "matriculaId", 10L,
                        "alunoId", 20L,
                        "turmaId", 30L,
                        "data", "2026-11-04")));
    }

    @Test
    void naoEstouraQuandoOBrokerEstaFora() {
        doThrow(new AmqpException("connection refused"))
                .when(rabbitTemplate).convertAndSend(any(), any(), any(Object.class));

        assertThatCode(() -> publisher.publicarMatriculaCriada(1L, 2L, 3L, LocalDate.now()))
                .doesNotThrowAnyException();
    }
}
