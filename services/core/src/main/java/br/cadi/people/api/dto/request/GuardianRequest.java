package br.cadi.people.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record GuardianRequest(
        @NotBlank(message = "Guardian name is required")
        String name,

        @NotBlank(message = "Guardian email is required")
        @Email(message = "Invalid guardian email format")
        String email,

        @NotBlank(message = "Guardian password is required")
        @Size(min = 6, message = "Guardian password must be at least 6 characters")
        String password,

        @NotBlank(message = "Guardian CPF is required")
        String cpf,

        @NotBlank(message = "Guardian phone is required")
        String phone,

        @NotNull(message = "Guardian birth date is required")
        LocalDate birthDate,

        @NotBlank(message = "Family tie is required")
        String familyTie,

        @NotNull(message = "Primary status is required")
        Boolean isPrimary,

        @NotNull(message = "Address data is required")
        @Valid
        AddressRequest address
) {}