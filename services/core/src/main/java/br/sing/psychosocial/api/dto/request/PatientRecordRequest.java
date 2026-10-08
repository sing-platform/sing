package br.sing.psychosocial.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Texto de uma anotação terapêutica do psicólogo sobre um paciente.
 */
public record PatientRecordRequest(
        @NotBlank(message = "A anotação não pode ser vazia")
        @Size(max = 5000, message = "A anotação deve ter no máximo 5000 caracteres")
        String notes
) {}
