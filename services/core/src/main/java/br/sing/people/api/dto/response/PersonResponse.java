package br.sing.people.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object representing a person")
public class PersonResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "João da Silva")
    private String name;

    @Schema(example = "joao.silva@example.com")
    private String email;

    @Schema(example = "+55 11 99999-9999")
    private String phone;

    @Schema(example = "123.456.789-00")
    private String cpf;

    @Schema(example = "1990-05-15")
    private LocalDate birthDate;

    @Schema(example = "São Paulo")
    private String city;

}
