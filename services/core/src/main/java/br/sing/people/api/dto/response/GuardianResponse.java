package br.sing.people.api.dto.response;

public record GuardianResponse(
        Long id,
        String name,
        String email,
        String phone,
        String familyTie,
        Boolean isPrimary
) {}