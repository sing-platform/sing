package br.sing.people.api.dto.response.addresses;

public record AddressResponseDTO(
        String zipCode,
        String street,
        Integer number,
        String neighborhood,
        String city,
        String state,
        String complement
) {}
