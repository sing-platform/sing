package br.sing.academic.application;

import br.sing.academic.domain.entities.MatriculaIntegracao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Depois do commit: publica o evento e chama o finance. Um falhar não desfaz o outro
 * nem a matrícula. O agendador fica fora do perfil de teste para não bater no H2.
 */
@Slf4j
@Service
@Profile("!test")
public class MatriculaIntegracaoWorker {

    private final MatriculaPendenciaGateway gateway;
    private final MatriculaEventoPublisher publisher;
    private final CobrancaClient cobrancaClient;
    private final N2Properties props;
    private final String replicaId;

    @Autowired
    public MatriculaIntegracaoWorker(
            MatriculaPendenciaGateway gateway,
            MatriculaEventoPublisher publisher,
            CobrancaClient cobrancaClient,
            N2Properties props) {
        this(gateway, publisher, cobrancaClient, props, replica());
    }

    MatriculaIntegracaoWorker(
            MatriculaPendenciaGateway gateway,
            MatriculaEventoPublisher publisher,
            CobrancaClient cobrancaClient,
            N2Properties props,
            String replicaId) {
        this.gateway = gateway;
        this.publisher = publisher;
        this.cobrancaClient = cobrancaClient;
        this.props = props;
        this.replicaId = replicaId;
    }

    @Scheduled(fixedDelayString = "${sing.n2.varredura-ms:5000}")
    public void varrer() {
        LocalDateTime agora = LocalDateTime.now();
        List<MatriculaIntegracao> linhas;
        try {
            linhas = gateway.reivindicar(replicaId, agora, props.getClaimSegundos());
        } catch (RuntimeException e) {
            log.warn("Não consegui reivindicar pendências de matrícula: {}", e.getMessage());
            return;
        }
        for (MatriculaIntegracao linha : linhas) {
            try {
                processar(linha, agora);
            } catch (RuntimeException e) {
                log.warn("Pendência {} falhou fora do previsto: {}", linha.getEventId(), e.getMessage());
            } finally {
                linha.setClaimedBy(null);
                linha.setClaimAte(null);
                try {
                    gateway.atualizar(linha);
                } catch (RuntimeException e) {
                    log.warn("Não gravei o resultado da pendência {}: {}", linha.getEventId(), e.getMessage());
                }
            }
        }
    }

    public void processar(MatriculaIntegracao linha, LocalDateTime agora) {
        if (MatriculaClaim.publicacaoPronta(linha, agora)) {
            try {
                publisher.publicar(linha.getEnvelope());
                linha.setPublicacao("concluida");
            } catch (RuntimeException e) {
                registrarFalhaPublicacao(linha, agora);
                log.warn("Publicação da matrícula {} adiada: {}", linha.getMatriculaId(), e.getMessage());
            }
        }
        if (MatriculaClaim.cobrancaPronta(linha, agora)) {
            try {
                Long id = cobrancaClient.criar(
                        linha.getMatriculaId(), linha.getValorCentavos(), linha.getVencimento());
                linha.setCobranca("concluida");
                linha.setCobrancaId(id);
            } catch (CobrancaConflitoException e) {
                linha.setCobranca("conflito");
                log.warn("Cobrança da matrícula {} em conflito", linha.getMatriculaId());
            } catch (RuntimeException e) {
                registrarFalhaCobranca(linha, agora);
                log.warn("Cobrança da matrícula {} adiada: {}", linha.getMatriculaId(), e.getMessage());
            }
        }
    }

    private void registrarFalhaPublicacao(MatriculaIntegracao linha, LocalDateTime agora) {
        int tentativas = linha.getTentativasPublicacao() + 1;
        linha.setTentativasPublicacao(tentativas);
        if (tentativas >= props.getRetryLimite()) {
            linha.setPublicacao("esgotada");
        } else {
            linha.setProximaPublicacao(agora.plus(props.getRetryIntervalo()));
        }
    }

    private void registrarFalhaCobranca(MatriculaIntegracao linha, LocalDateTime agora) {
        int tentativas = linha.getTentativasCobranca() + 1;
        linha.setTentativasCobranca(tentativas);
        if (tentativas >= props.getRetryLimite()) {
            linha.setCobranca("esgotada");
        } else {
            linha.setProximaCobranca(agora.plus(props.getRetryIntervalo()));
        }
    }

    static String replica() {
        String definida = System.getenv("N2_REPLICA_ID");
        if (definida != null && !definida.isBlank()) {
            return definida;
        }
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return host + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
