package br.cadi.document.api.dto.response;

import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.document.domain.enums.DocumentScope;
import br.cadi.document.domain.enums.DocumentStatus;
import br.cadi.document.domain.entities.UserDocument;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        Long ownerPersonId,
        String originalFileName,
        String mimeType,
        Long fileSize,
        DocumentCategory category,
        DocumentScope scope,
        String description,
        Instant uploadedAt,
        // Fluxo de aprovacao (relevante para ATESTADO_MEDICO / JUSTIFICATIVA_FALTA)
        DocumentStatus status,
        LocalDate referenceDateStart,
        LocalDate referenceDateEnd,
        Long reviewedByUserId,
        Instant reviewedAt,
        String rejectionReason
) {
    public static DocumentResponse from(UserDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getOwnerPersonId(),
                doc.getOriginalFileName(),
                doc.getMimeType(),
                doc.getFileSize(),
                doc.getCategory(),
                doc.getScope(),
                doc.getDescription(),
                doc.getUploadedAt(),
                doc.getStatus(),
                doc.getReferenceDateStart(),
                doc.getReferenceDateEnd(),
                doc.getReviewedByUserId(),
                doc.getReviewedAt(),
                doc.getRejectionReason()
        );
    }
}
