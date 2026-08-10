package br.cadi.communication.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Notificação in-app do usuário")
public record NotificationResponse(
        @Schema(description = "Id da notificação") Long id,
        @Schema(description = "Título") String title,
        @Schema(description = "Mensagem") String message,
        @Schema(description = "Tipo") String type,
        @Schema(description = "URL de ação ao clicar") String actionUrl,
        @Schema(description = "Data de leitura; null se não lida") LocalDateTime readAt,
        @Schema(description = "Data de criação") LocalDateTime createdAt
) {
}
