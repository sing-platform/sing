package br.sing.academic.application;

import br.sing.academic.domain.entities.MatriculaIntegracao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MatriculaIntegracaoWorkerTest {

    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 5, 18, 30);
    private N2Properties props;
    private GravadorPublicacao publicacao;
    private GravadorCobranca cobranca;
    private MatriculaIntegracaoWorker worker;

    @BeforeEach
    void setUp() {
        props = new N2Properties();
        props.setRetryIntervalo(Duration.ofSeconds(30));
        props.setRetryLimite(3);
        props.setClaimSegundos(60);
        publicacao = new GravadorPublicacao();
        cobranca = new GravadorCobranca();
        worker = new MatriculaIntegracaoWorker(null, publicacao, cobranca, props, "replica-a");
    }

    @Test
    void financeForaEDepoisUmaCobrancaNaMesmaPendencia() {
        MatriculaIntegracao linha = pendente();
        cobranca.falhasAntesDeOk = 1;

        worker.processar(linha, agora);

        assertThat(linha.getPublicacao()).isEqualTo("concluida");
        assertThat(linha.getCobranca()).isEqualTo("pendente");
        assertThat(linha.getTentativasCobranca()).isEqualTo(1);
        assertThat(linha.getEventId()).isEqualTo("11111111-1111-4111-8111-111111111111");

        worker.processar(linha, agora.plusSeconds(30));

        assertThat(linha.getCobranca()).isEqualTo("concluida");
        assertThat(linha.getCobrancaId()).isEqualTo(44001L);
        assertThat(cobranca.matriculas).containsExactly(9001L, 9001L);
        assertThat(linha.getEventId()).isEqualTo("11111111-1111-4111-8111-111111111111");
        assertThat(publicacao.envelopes).hasSize(1);
    }

    @Test
    void conflitoNaoEntraEmRetry() {
        MatriculaIntegracao linha = pendente();
        cobranca.conflito = true;

        worker.processar(linha, agora);
        worker.processar(linha, agora.plusMinutes(5));

        assertThat(linha.getCobranca()).isEqualTo("conflito");
        assertThat(cobranca.chamadas()).isEqualTo(1);
        assertThat(linha.getEventId()).isEqualTo("11111111-1111-4111-8111-111111111111");
    }

    @Test
    void tentativaEsgotadaFicaVisivel() {
        MatriculaIntegracao linha = pendente();
        publicacao.falha = true;
        cobranca.sempreIndisponivel = true;

        worker.processar(linha, agora);
        worker.processar(linha, agora.plusSeconds(30));
        worker.processar(linha, agora.plusSeconds(60));

        assertThat(linha.getPublicacao()).isEqualTo("esgotada");
        assertThat(linha.getCobranca()).isEqualTo("esgotada");
        assertThat(linha.getTentativasPublicacao()).isEqualTo(3);
        assertThat(linha.getTentativasCobranca()).isEqualTo(3);

        int publicacoes = publicacao.envelopes.size();
        int cobrancas = cobranca.chamadas();
        worker.processar(linha, agora.plusMinutes(10));
        assertThat(publicacao.envelopes).hasSize(publicacoes);
        assertThat(cobranca.chamadas()).isEqualTo(cobrancas);
    }

    @Test
    void esperaOIntervaloAntesDeTentarDeNovo() {
        MatriculaIntegracao linha = pendente();
        publicacao.falha = true;

        worker.processar(linha, agora);
        worker.processar(linha, agora.plusSeconds(10));

        assertThat(publicacao.envelopes).hasSize(1);
        assertThat(linha.getPublicacao()).isEqualTo("pendente");
    }

    @Test
    void claimVivoBloqueiaOutraReplica() {
        MatriculaIntegracao linha = pendente();
        assertThat(MatriculaClaim.linhaPronta(linha, agora)).isTrue();

        linha.setClaimedBy("replica-a");
        linha.setClaimAte(agora.plusSeconds(60));
        assertThat(MatriculaClaim.linhaPronta(linha, agora.plusSeconds(10))).isFalse();
        assertThat(MatriculaClaim.linhaPronta(linha, agora.plusSeconds(61))).isTrue();
    }

    @Test
    void sqlDoClaimUsaSkipLocked() {
        assertThat(br.sing.academic.infra.MatriculaPendenciaJpaGateway.SQL_REIVINDICAR)
                .contains("FOR UPDATE SKIP LOCKED")
                .contains("claim_ate")
                .contains("publicacao = 'pendente'")
                .contains("cobranca = 'pendente'");
        assertThat(br.sing.academic.infra.MatriculaPendenciaJpaGateway.SQL_MARCAR)
                .contains("claimed_by");
    }

    private MatriculaIntegracao pendente() {
        return MatriculaIntegracao.builder()
                .id(1L)
                .matriculaId(9001L)
                .eventId("11111111-1111-4111-8111-111111111111")
                .envelope("{\"eventId\":\"11111111-1111-4111-8111-111111111111\"}")
                .valorCentavos(15000)
                .vencimento(LocalDate.of(2026, 11, 15))
                .publicacao("pendente")
                .cobranca("pendente")
                .build();
    }

    private static final class GravadorPublicacao implements MatriculaEventoPublisher {
        private final List<String> envelopes = new ArrayList<>();
        private boolean falha;

        @Override
        public void publicar(String envelopeJson) {
            envelopes.add(envelopeJson);
            if (falha) {
                throw new RuntimeException("broker fora");
            }
        }
    }

    private static final class GravadorCobranca extends CobrancaClient {
        private final List<Long> matriculas = new ArrayList<>();
        private final AtomicInteger chamadasContadas = new AtomicInteger();
        private int falhasAntesDeOk;
        private boolean conflito;
        private boolean sempreIndisponivel;

        private GravadorCobranca() {
            super(org.springframework.web.client.RestClient.builder().baseUrl("http://127.0.0.1:9").build());
        }

        @Override
        public Long criar(long matriculaId, int valorCentavos, LocalDate vencimento) {
            chamadasContadas.incrementAndGet();
            matriculas.add(matriculaId);
            if (conflito) {
                throw new CobrancaConflitoException();
            }
            if (sempreIndisponivel || chamadasContadas.get() <= falhasAntesDeOk) {
                throw new CobrancaIndisponivelException("finance fora");
            }
            return 44001L;
        }

        private int chamadas() {
            return chamadasContadas.get();
        }
    }
}
