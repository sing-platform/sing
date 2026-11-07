package br.sing.academic.api.dto.response;

public record MatriculaItemResponse(
        Long matriculaId,
        String eventId,
        String publicacao,
        String cobranca
) {}
