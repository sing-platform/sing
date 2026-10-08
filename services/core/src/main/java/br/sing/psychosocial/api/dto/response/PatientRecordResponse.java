package br.sing.psychosocial.api.dto.response;

import br.sing.psychosocial.domain.PatientRecord;

import java.time.LocalDateTime;

/**
 * Anotação terapêutica retornada ao psicólogo autor.
 */
public record PatientRecordResponse(
        Long id,
        Long patientId,
        String notes,
        LocalDateTime createdAt
) {
    public static PatientRecordResponse from(PatientRecord record) {
        return new PatientRecordResponse(
                record.getId(),
                record.getPatient().getId(),
                record.getNotes(),
                record.getCreatedAt()
        );
    }
}
