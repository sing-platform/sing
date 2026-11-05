package br.sing.academic.infra;

import br.sing.academic.application.MatriculaPendenciaGateway;
import br.sing.academic.domain.entities.MatriculaIntegracao;
import br.sing.academic.infra.repositories.MatriculaIntegracaoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Claim com {@code FOR UPDATE SKIP LOCKED}: duas réplicas não levam a mesma linha.
 * A transação é curta de propósito — publicar e cobrar ficam fora dela.
 */
@Repository
public class MatriculaPendenciaJpaGateway implements MatriculaPendenciaGateway {

    public static final String SQL_MARCAR = """
            UPDATE matricula_integracao
            SET claimed_by = :replica, claim_ate = :ate
            WHERE id IN (:ids)
            """;

    public static final String SQL_REIVINDICAR = """
            SELECT id FROM matricula_integracao
            WHERE (claim_ate IS NULL OR claim_ate < :agora)
              AND (
                    (publicacao = 'pendente' AND (proxima_publicacao IS NULL OR proxima_publicacao <= :agora))
                 OR (cobranca = 'pendente' AND (proxima_cobranca IS NULL OR proxima_cobranca <= :agora))
              )
            ORDER BY id
            FOR UPDATE SKIP LOCKED
            LIMIT 20
            """;

    private final MatriculaIntegracaoRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public MatriculaPendenciaJpaGateway(MatriculaIntegracaoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<MatriculaIntegracao> reivindicar(String replica, LocalDateTime agora, int claimSegundos) {
        @SuppressWarnings("unchecked")
        List<Object> linhas = entityManager.createNativeQuery(SQL_REIVINDICAR)
                .setParameter("agora", Timestamp.valueOf(agora))
                .getResultList();
        if (linhas.isEmpty()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>();
        for (Object linha : linhas) {
            ids.add(idDe(linha));
        }

        entityManager.createNativeQuery(SQL_MARCAR)
                .setParameter("replica", replica)
                .setParameter("ate", Timestamp.valueOf(agora.plusSeconds(claimSegundos)))
                .setParameter("ids", ids)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return repository.findAllById(ids);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void atualizar(MatriculaIntegracao linha) {
        repository.save(linha);
    }

    private static long idDe(Object linha) {
        if (linha instanceof Number numero) {
            return numero.longValue();
        }
        if (linha instanceof Object[] colunas && colunas[0] instanceof Number numero) {
            return numero.longValue();
        }
        throw new IllegalStateException("Id inesperado ao reivindicar pendência: " + linha);
    }
}
