package br.sing.academic.api.dto.response;

public record IntegracaoMatriculaResponse(
        Long matriculaId,
        String eventId,
        String publicacao,
        String cobranca,
        int tentativasPublicacao,
        int tentativasCobranca
) {}
