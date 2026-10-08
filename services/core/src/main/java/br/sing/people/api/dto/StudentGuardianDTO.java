package br.sing.people.api.dto;

import br.sing.academic.api.dto.response.LectureStudentResponse;
import br.sing.people.api.dto.response.addresses.AddressDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class StudentGuardianDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String cpf;
    private AddressDTO address;
    private List<LectureStudentResponse> lectures;

}
