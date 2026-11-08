package br.sing.audience.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Autor e destinatários resolvidos para uma publicação do mural")
public record AudienceResponse(Author author, List<Recipient> recipients) {

    public record Author(Long userId, String name, String email, String role) {}

    public record Recipient(Long userId, String email) {}
}
