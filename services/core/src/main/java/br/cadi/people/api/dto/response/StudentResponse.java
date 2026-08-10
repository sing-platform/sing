package br.cadi.people.api.dto.response;

import br.cadi.people.api.dto.response.addresses.AddressResponseDTO;

import java.time.LocalDate;
import java.util.List;

public record StudentResponse(
        Long id,
        String name,
        String email,
        String cpf,
        String phone,
        String grade,
        String currentSchool,
        String periodStudies,
        LocalDate birthDate,
        AddressResponseDTO address,
        List<GuardianResponse> guardians,
        String chronicIllness,
        String allergy,
        String medicine,
        String deficiency,
        String followUp,
        String surgery
) {}