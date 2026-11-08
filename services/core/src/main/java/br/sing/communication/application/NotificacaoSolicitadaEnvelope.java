package br.sing.communication.application;

import java.util.List;

public record NotificacaoSolicitadaEnvelope(
        String eventId,
        String eventType,
        int version,
        String occurredAt,
        Payload payload
) {

    public record Payload(
            String type,
            String title,
            String message,
            String actionUrl,
            String referenceKey,
            List<Long> recipientUserIds
    ) {}
}
