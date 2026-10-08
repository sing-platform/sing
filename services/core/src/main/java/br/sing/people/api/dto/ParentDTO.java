package br.sing.people.api.dto;

import br.sing.people.api.dto.response.addresses.AddressDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Data
@SuperBuilder
@RequiredArgsConstructor
@AllArgsConstructor
public class ParentDTO {

    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String cpf;
    private AddressDTO address;
    private String role;

    // Lista de filhos (students)
    private List<StudentGuardianDTO> students;
}