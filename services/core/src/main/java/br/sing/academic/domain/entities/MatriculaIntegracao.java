package br.sing.academic.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pendência de publicação e de cobrança de uma matrícula criada.
 * Não é o outbox de e-mail ({@code message_outbox}).
 */
@Entity
@Table(name = "matricula_integracao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatriculaIntegracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula_id", nullable = false, unique = true)
    private Long matriculaId;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String envelope;

    @Column(name = "valor_centavos", nullable = false)
    private int valorCentavos;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false, length = 20)
    private String publicacao;

    @Column(nullable = false, length = 20)
    private String cobranca;

    @Column(name = "tentativas_publicacao", nullable = false)
    private int tentativasPublicacao;

    @Column(name = "tentativas_cobranca", nullable = false)
    private int tentativasCobranca;

    @Column(name = "proxima_publicacao")
    private LocalDateTime proximaPublicacao;

    @Column(name = "proxima_cobranca")
    private LocalDateTime proximaCobranca;

    @Column(name = "claimed_by", length = 120)
    private String claimedBy;

    @Column(name = "claim_ate")
    private LocalDateTime claimAte;

    @Column(name = "cobranca_id")
    private Long cobrancaId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void preencherCriadoEm() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
