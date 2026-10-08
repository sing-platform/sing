package br.sing.people.api.dto.response.addresses;

import br.sing.people.domain.entities.Address;

public record AddressResponse(
        Long id,
        String zipCode,
        String street,
        Integer number,
        String complement,
        String neighborhood,
        String city,
        String state
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getZipCode(),
                a.getStreet(),
                a.getNumber(),
                a.getComplement(),
                a.getNeighborhood(),
                a.getCity(),
                a.getState()
        );
    }
}