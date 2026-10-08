package br.sing.communication.domain;

import lombok.*;

/**
 * Evento de e-mail enviado pelo RabbitMQ (routing key {@code email.send}).
 *
 * <p>Serializado como JSON pela {@link com.fasterxml.jackson.databind.ObjectMapper}
 * configurada em {@link br.sing.config.RabbitMQConfig}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageEvent {

    /** Endereço de destino. */
    private String to;

    /** Assunto do e-mail. */
    private String subject;

    /** Corpo do e-mail. */
    private String body;

    /** {@code true} → corpo em HTML; {@code false} → texto puro. */
    @Builder.Default
    private boolean html = true;
}
