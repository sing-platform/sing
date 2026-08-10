package br.cadi.people.api.dto.request;

import br.cadi.people.domain.enums.CollaboratorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CollaboratorCreateRequest(

        @NotNull(message = "Collaborator type is mandatory")
        CollaboratorType type,

        @NotBlank(message = "Name is mandatory")
        String name,

        @NotBlank(message = "CPF is mandatory")
        @Size(min = 11, max = 14, message = "CPF inválido")
        String cpf,

        @NotBlank(message = "E-mail is mandatory")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone is mandatory")
        String phone,

        @NotNull(message = "Birth date is mandatory")
        @Past(message = "Date of birth must be in the past tense")
        LocalDate birthDate,

        @Valid
        @NotNull(message = "Address is mandatory")
        AddressRequest address,

        @NotBlank(message = "Education level is mandatory")
        @Size(max = 50)
        String educationLevel,

        String educationCertType,
        String institutionCert,
        Integer yearConclusion,
        String department
) {}
