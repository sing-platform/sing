package br.sing.academic.infra.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publica o evento {@code matricula.criada} na exchange topic {@code sing.events}.
 * Se o RabbitMQ estiver fora, a matrícula já gravada não pode falhar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatriculaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${sing.events.exchange:sing.events}")
    private String exchange;

    public void publicarMatriculaCriada(Long matriculaId, Long alunoId, Long turmaId, LocalDate data) {
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("matriculaId", matriculaId);
        evento.put("alunoId", alunoId);
        evento.put("turmaId", turmaId);
        evento.put("data", data == null ? null : data.toString());

        try {
            rabbitTemplate.convertAndSend(exchange, EventsRabbitConfig.MATRICULA_CRIADA_RK, evento);
            log.info("Evento matricula.criada publicado: {}", evento);
        } catch (Exception e) {
            log.warn("RabbitMQ indisponivel. Matricula {} foi salva, mas o evento matricula.criada nao saiu: {}",
                    matriculaId, e.getMessage());
        }
    }
}
