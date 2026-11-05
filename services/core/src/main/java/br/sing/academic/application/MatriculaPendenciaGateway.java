package br.sing.academic.application;

import br.sing.academic.domain.entities.MatriculaIntegracao;

import java.time.LocalDateTime;
import java.util.List;

public interface MatriculaPendenciaGateway {

    /**
     * Reserva linhas prontas para esta réplica. Quem não ganha o claim recebe lista vazia.
     */
    List<MatriculaIntegracao> reivindicar(String replica, LocalDateTime agora, int claimSegundos);

    void atualizar(MatriculaIntegracao linha);
}
