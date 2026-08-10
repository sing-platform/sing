package br.cadi.people.api.dto.response;

import br.cadi.people.api.dto.response.addresses.AddressResponse;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.enums.CollaboratorType;

import java.time.LocalDate;

public record CollaboratorDetailResponse(
        Long id,
        CollaboratorType type,
        String name,
        String cpf,
        String email,
        String phone,
        LocalDate birthDate,
        AddressResponse address,
        String educationLevel,
        String educationCertType,
        String institutionCert,
        Integer yearConclusion,
        String department,
        Long userId,
        boolean active
) {
    public static CollaboratorDetailResponse from(Collaborator c) {
        return new CollaboratorDetailResponse(
                c.getId(),
                c.getType(),
                c.getName(),
                c.getCpf(),
                c.getEmail(),
                c.getPhone(),
                c.getBirthDate(),
                AddressResponse.from(c.getAddress()),
                c.getEducationLevel(),
                c.getEducationCertType(),
                c.getInstitutionCert(),
                c.getYearConclusion(),
                c.getDepartment(),
                c.getUser().getId(),
                c.getUser().isActive()
        );
    }
}
