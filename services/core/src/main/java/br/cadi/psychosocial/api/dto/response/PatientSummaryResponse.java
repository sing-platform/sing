package br.cadi.psychosocial.api.dto.response;

import br.cadi.people.domain.entities.Student;

import java.time.LocalDate;
import java.time.Period;

/**
 * Resumo de um paciente vinculado, para a lista do psicólogo.
 */
public record PatientSummaryResponse(
        Long id,
        String name,
        Integer age,
        String grade
) {
    public static PatientSummaryResponse from(Student student) {
        return new PatientSummaryResponse(
                student.getId(),
                student.getName(),
                ageOf(student.getBirthDate()),
                student.getGrade()
        );
    }

    private static Integer ageOf(LocalDate birthDate) {
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
    }
}
