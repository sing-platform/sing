package br.sing.people.api.dto.response;

import br.sing.people.api.dto.response.addresses.AddressDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaboratorResponse {

    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String cpf;
    private AddressDTO address;
    private String role;
    private  String educationLevel;
    private String CollaboratorType;
    /** ID do colaborador (person_id); útil para o frontend evitar listagens completas (ex.: pareceres). */
    private Long collaboratorId;

}
