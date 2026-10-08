package br.sing.communication.infra.repositories;

import br.sing.communication.domain.MessageOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageOutboxRepository extends JpaRepository<MessageOutbox, Long> {

    /** Retorna todas as mensagens ainda não processadas para reenvio. */
    List<MessageOutbox> findByProcessedFalse();
}
