package br.sing.people.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record StudentUpdateRequest(
        @NotBlank(message = "Student name is required")
        String name,

        @NotBlank(message = "Student email is required")
        @Email(message = "Invalid student email format")
        String email,

        @NotBlank(message = "Student CPF is required")
        String cpf,

        @NotBlank(message = "Student phone is required")
        String phone,

        @NotNull(message = "Student birth date is required")
        LocalDate birthDate,

        @NotBlank(message = "Grade is required")
        String grade,

        @NotBlank(message = "Current school is required")
        String currentSchool,

        @NotBlank(message = "Period studies is required")
        String periodStudies,

        @NotNull(message = "Address data is required")
        @Valid
        AddressRequest address,

        String chronicIllness,
        String allergy,
        String medicine,
        String deficiency,
        String followUp,
        String surgery
) {}
