package br.sing.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Aluno matriculado na turma (dados mínimos pedagógicos/operacionais)")
public record SchoolRoomStudentResponse(
        @Schema(description = "ID do aluno (person_id)") Long studentId,
        String name,
        String email,
        String phone,
        String grade,
        @Schema(description = "Período de estudo (ex.: MATUTINO)") String periodStudies,
        LocalDate enrollmentDate,
        boolean enrollmentActive
) {}
