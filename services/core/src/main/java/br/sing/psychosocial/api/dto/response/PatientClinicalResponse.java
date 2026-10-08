package br.sing.psychosocial.api.dto.response;

import br.sing.people.domain.entities.Student;

import java.time.LocalDate;
import java.time.Period;

/**
 * Dados clínicos do paciente em modo somente leitura para o psicólogo.
 * Os campos clínicos são mantidos no cadastro do aluno (pela secretaria).
 */
public record PatientClinicalResponse(
        Long id,
        String name,
        Integer age,
        LocalDate birthDate,
        String grade,
        String currentSchool,
        String periodStudies,
        String chronicIllness,
        String allergy,
        String medicine,
        String deficiency,
        String followUp,
        String surgery
) {
    public static PatientClinicalResponse from(Student s) {
        Integer age = s.getBirthDate() == null
                ? null
                : Period.between(s.getBirthDate(), LocalDate.now()).getYears();
        return new PatientClinicalResponse(
                s.getId(),
                s.getName(),
                age,
                s.getBirthDate(),
                s.getGrade(),
                s.getCurrentSchool(),
                s.getPeriodStudies(),
                s.getChronicIllness(),
                s.getAllergy(),
                s.getMedicine(),
                s.getDeficiency(),
                s.getFollowUp(),
                s.getSurgery()
        );
    }
}
