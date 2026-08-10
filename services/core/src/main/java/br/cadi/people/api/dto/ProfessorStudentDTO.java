package br.cadi.people.api.dto;

import lombok.*;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProfessorStudentDTO {
    String name;
    String surname;
    String email;
}
