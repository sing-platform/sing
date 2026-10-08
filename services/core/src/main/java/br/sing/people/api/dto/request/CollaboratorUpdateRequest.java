package br.sing.people.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CollaboratorUpdateRequest(
        @NotBlank(message = "Name is mandatory")
        String name,

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
