package br.sing.academic.application;

import br.sing.academic.domain.entities.MatriculaIntegracao;

import java.time.LocalDateTime;

/**
 * Regra de quando uma pendência pode ser pega por uma réplica.
 * O SQL de {@link br.sing.academic.infra.MatriculaPendenciaJpaGateway} repete esta regra.
 */
public final class MatriculaClaim {

    private MatriculaClaim() {
    }

    public static boolean publicacaoPronta(MatriculaIntegracao linha, LocalDateTime agora) {
        return "pendente".equals(linha.getPublicacao()) && horarioChegou(linha.getProximaPublicacao(), agora);
    }

    public static boolean cobrancaPronta(MatriculaIntegracao linha, LocalDateTime agora) {
        return "pendente".equals(linha.getCobranca()) && horarioChegou(linha.getProximaCobranca(), agora);
    }

    public static boolean linhaPronta(MatriculaIntegracao linha, LocalDateTime agora) {
        if (!publicacaoPronta(linha, agora) && !cobrancaPronta(linha, agora)) {
            return false;
        }
        return linha.getClaimAte() == null || linha.getClaimAte().isBefore(agora);
    }

    private static boolean horarioChegou(LocalDateTime proxima, LocalDateTime agora) {
        return proxima == null || !proxima.isAfter(agora);
    }
}
