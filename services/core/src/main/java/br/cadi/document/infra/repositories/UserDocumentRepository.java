package br.cadi.document.infra.repositories;

import br.cadi.document.domain.entities.UserDocument;
import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.document.domain.enums.DocumentScope;
import br.cadi.document.domain.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    List<UserDocument> findByOwnerPersonIdAndActiveTrue(Long ownerPersonId);

    List<UserDocument> findByOwnerPersonIdAndScopeAndActiveTrue(Long ownerPersonId, DocumentScope scope);

    List<UserDocument> findByUploadedByUserIdAndScopeAndActiveTrue(Long uploadedByUserId, DocumentScope scope);

    // --- Fluxo de aprovacao de atestados ---

    /** Lista documentos (atestados/justificativas) por status, para a secretaria triar. */
    List<UserDocument> findByStatusAndCategoryInAndActiveTrueOrderByUploadedAtAsc(
            DocumentStatus status, Collection<DocumentCategory> categories);

    /** Lista todos (sem filtrar status) para a aba "Todos" da secretaria. */
    List<UserDocument> findByCategoryInAndActiveTrueOrderByUploadedAtDesc(
            Collection<DocumentCategory> categories);

    /**
     * Verifica se ja existe atestado/justificativa APROVADO cobrindo a data informada
     * para o aluno. Usado para auto-justificar faltas registradas depois da aprovacao.
     */
    @Query("""
        SELECT COUNT(d) > 0 FROM UserDocument d
        WHERE d.ownerPersonId = :personId
          AND d.active = true
          AND d.status = br.cadi.document.domain.enums.DocumentStatus.APPROVED
          AND d.category IN (
                br.cadi.document.domain.enums.DocumentCategory.ATESTADO_MEDICO,
                br.cadi.document.domain.enums.DocumentCategory.JUSTIFICATIVA_FALTA)
          AND :date BETWEEN d.referenceDateStart AND d.referenceDateEnd
    """)
    boolean existsApprovedCoveringDate(@Param("personId") Long personId, @Param("date") LocalDate date);

    /**
     * Retorna o atestado APROVADO mais recente que cobre a data, se houver.
     * Permite gravar o vinculo (justified_by_document_id) na auto-justificativa.
     */
    @Query("""
        SELECT d FROM UserDocument d
        WHERE d.ownerPersonId = :personId
          AND d.active = true
          AND d.status = br.cadi.document.domain.enums.DocumentStatus.APPROVED
          AND d.category IN (
                br.cadi.document.domain.enums.DocumentCategory.ATESTADO_MEDICO,
                br.cadi.document.domain.enums.DocumentCategory.JUSTIFICATIVA_FALTA)
          AND :date BETWEEN d.referenceDateStart AND d.referenceDateEnd
        ORDER BY d.reviewedAt DESC
    """)
    List<UserDocument> findApprovedCoveringDate(@Param("personId") Long personId, @Param("date") LocalDate date);
}
