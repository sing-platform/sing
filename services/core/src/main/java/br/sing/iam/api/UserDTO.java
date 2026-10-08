package br.sing.iam.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


public record UserDTO(    @Schema(example = "joao123") String username,
                          @Schema(example = "ROLE_PROFESSOR") String role) {
}
