package br.sing.communication.infra.repositories;

import br.sing.communication.domain.MessageOutbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de slice JPA para {@link MessageOutboxRepository}.
 *
 * <p>Profile {@code test} (ver {@code application-test.yml}) usa H2 em memória
 * com {@code ddl-auto=create-drop} e Flyway desligado.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MessageOutboxRepositoryTest {

    @Autowired
    private MessageOutboxRepository repository;

    @Test
    void findByProcessedFalseDeveRetornarApenasMensagensNaoProcessadas() {
        repository.save(MessageOutbox.builder()
                .exchange("ex")
                .routingKey("email.send")
                .payload("{}")
                .processed(false)
                .build());
        repository.save(MessageOutbox.builder()
                .exchange("ex")
                .routingKey("email.send")
                .payload("{}")
                .processed(true)
                .processedAt(LocalDateTime.now())
                .build());
        repository.save(MessageOutbox.builder()
                .exchange("ex")
                .routingKey("email.send")
                .payload("{}")
                .processed(false)
                .build());

        List<MessageOutbox> pending = repository.findByProcessedFalse();

        assertThat(pending).hasSize(2);
        assertThat(pending).allMatch(m -> !m.isProcessed());
    }

    @Test
    void findByProcessedFalseDeveRetornarVazioQuandoTudoProcessado() {
        repository.save(MessageOutbox.builder()
                .exchange("ex")
                .routingKey("email.send")
                .payload("{}")
                .processed(true)
                .processedAt(LocalDateTime.now())
                .build());

        assertThat(repository.findByProcessedFalse()).isEmpty();
    }
}
