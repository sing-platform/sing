package br.cadi.people.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtro por quantidade de matrículas ativas do aluno")
public enum StudentEnrollmentFilter {
    @Schema(description = "Sem filtro por quantidade de matrículas")
    ANY,
    @Schema(description = "Sem matrícula ativa")
    NONE,
    @Schema(description = "Exatamente uma matrícula ativa")
    ONE,
    @Schema(description = "Duas matrículas ativas (limite)")
    TWO
}
