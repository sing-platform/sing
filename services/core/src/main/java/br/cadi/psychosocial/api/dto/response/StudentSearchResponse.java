package br.cadi.psychosocial.api.dto.response;

import br.cadi.people.domain.entities.Student;

import java.time.LocalDate;
import java.time.Period;

/**
 * Item de busca de alunos para auto-vinculação. {@code linked} indica se o
 * aluno já é paciente do psicólogo autenticado.
 */
public record StudentSearchResponse(
        Long id,
        String name,
        Integer age,
        String grade,
        boolean linked
) {
    public static StudentSearchResponse from(Student student, boolean linked) {
        LocalDate birthDate = student.getBirthDate();
        Integer age = birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
        return new StudentSearchResponse(student.getId(), student.getName(), age, student.getGrade(), linked);
    }
}
