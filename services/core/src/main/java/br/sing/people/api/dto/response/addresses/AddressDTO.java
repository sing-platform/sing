package br.sing.people.api.dto.response.addresses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class AddressDTO {
    String street;
    String number;
    String city;
    String state;
    String zip;
    String complement;
    String neighborhood;

}
