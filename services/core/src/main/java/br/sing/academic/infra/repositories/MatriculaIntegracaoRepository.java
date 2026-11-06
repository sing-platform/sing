package br.sing.academic.infra.repositories;

import br.sing.academic.domain.entities.MatriculaIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatriculaIntegracaoRepository extends JpaRepository<MatriculaIntegracao, Long> {

    Optional<MatriculaIntegracao> findByMatriculaId(Long matriculaId);
}
