package br.cadi.document.domain.entities;

import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.document.domain.enums.DocumentScope;
import br.cadi.document.domain.enums.DocumentStatus;
import br.cadi.document.domain.exception.AtestadoOperationException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_documents")
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_person_id", nullable = false)
    private Long ownerPersonId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentScope scope;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // --- Fluxo de aprovacao (apenas para atestados / justificativas) ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "reference_date_start")
    private LocalDate referenceDateStart;

    @Column(name = "reference_date_end")
    private LocalDate referenceDateEnd;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Marca o documento como aprovado pelo revisor para o periodo informado. */
    public void approve(Long reviewerUserId, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new AtestadoOperationException("Periodo de referencia obrigatorio para aprovacao.");
        }
        if (end.isBefore(start)) {
            throw new AtestadoOperationException("Data final nao pode ser anterior a data inicial.");
        }
        this.status = DocumentStatus.APPROVED;
        this.referenceDateStart = start;
        this.referenceDateEnd = end;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = Instant.now();
        this.rejectionReason = null;
    }

    /** Marca o documento como rejeitado. Motivo e obrigatorio. */
    public void reject(Long reviewerUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AtestadoOperationException("Motivo da rejeicao e obrigatorio.");
        }
        this.status = DocumentStatus.REJECTED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = Instant.now();
        this.rejectionReason = reason;
    }
}
