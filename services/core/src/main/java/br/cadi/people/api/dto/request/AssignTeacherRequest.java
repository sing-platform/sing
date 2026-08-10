package br.cadi.people.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Corpo para **PUT /api/v1/schoolrooms/{id}/teacher**")
public record AssignTeacherRequest(
        @NotNull(message = "Teacher ID is mandatory")
        @Schema(description = "ID do colaborador professor (titular)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Long teacherId
) {}
