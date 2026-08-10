package br.cadi.people.api.dto.response;

import br.cadi.people.domain.enums.CollaboratorType;

import java.util.Map;

public record CollaboratorStatsResponse(
        long total,
        long totalActive,
        Map<CollaboratorType, Long> byType,
        Map<CollaboratorType, Long> activeByType
) {}
