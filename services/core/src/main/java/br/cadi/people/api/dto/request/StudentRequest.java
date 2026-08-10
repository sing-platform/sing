package br.cadi.people.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record StudentRequest(
        @NotBlank(message = "Student name is required")
        String name,

        @NotBlank(message = "Student email is required")
        @Email(message = "Invalid student email format")
        String email,

        @NotBlank(message = "Student password is required")
        @Size(min = 6, message = "Student password must be at least 6 characters")
        String password,

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

        @NotEmpty(message = "At least one guardian is required")
        @Valid
        List<GuardianRequest> guardians,

        String chronicIllness,
        String allergy,
        String medicine,
        String deficiency,
        String followUp,
        String surgery
) {}