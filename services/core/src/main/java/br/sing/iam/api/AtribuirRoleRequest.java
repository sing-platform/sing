package br.sing.iam.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record AtribuirRoleRequest(
        @Schema(example = "joao123") String username,
        @Schema(example = "ROLE_PROFESSOR") String role
) {}