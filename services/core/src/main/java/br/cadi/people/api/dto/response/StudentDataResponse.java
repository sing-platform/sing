package br.cadi.people.api.dto.response;

import br.cadi.academic.api.dto.response.LectureStudentResponse;
import br.cadi.people.api.dto.response.addresses.AddressDTO;
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
public class StudentDataResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private AddressDTO address;
    private String role;
    private List<LectureStudentResponse> lectures;
    private List<String> guardiansNames;

}
