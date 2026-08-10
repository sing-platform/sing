package br.cadi.config;

import java.time.LocalDate;

record SeedPerson(
        String email,
        String username,
        String role,
        String firstName,
        String lastName,
        String cpf,
        String periodStudies,
        LocalDate birthDate
) {
    SeedPerson(String email, String username, String role, String firstName, String lastName, String cpf) {
        this(email, username, role, firstName, lastName, cpf, null, LocalDate.of(1990, 5, 15));
    }

    SeedPerson(String email, String username, String role, String firstName, String lastName,
               String cpf, String periodStudies) {
        this(email, username, role, firstName, lastName, cpf, periodStudies, LocalDate.of(2010, 3, 20));
    }
}
