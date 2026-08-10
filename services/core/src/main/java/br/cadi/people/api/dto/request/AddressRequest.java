package br.cadi.people.api.dto.request;

import jakarta.validation.constraints.*;

public record AddressRequest(
        @NotBlank(message = "CEP é obrigatório")
        String zipCode,

        @NotBlank(message = "Rua é obrigatória")
        String street,

        @NotNull(message = "Número é obrigatório")
        Integer number,

        @NotBlank(message = "Bairro é obrigatório")
        String neighborhood,

        @NotBlank(message = "Cidade é obrigatória")
        String city,

        @NotBlank(message = "Estado (UF) é obrigatório")
        String state,

        String complement // opcional
) {}