package br.cadi.document.application;

import br.cadi.academic.domain.entities.Attendance;
import br.cadi.academic.domain.enums.AttendanceStatus;
import br.cadi.academic.infra.repositories.AttendanceRepository;
import br.cadi.document.api.dto.response.DocumentResponse;
import br.cadi.document.domain.entities.UserDocument;
import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.document.domain.enums.DocumentStatus;
import br.cadi.document.domain.exception.AtestadoOperationException;
import br.cadi.document.domain.exception.DocumentNotFoundException;
import br.cadi.document.infra.repositories.UserDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Servico que coordena o fluxo de aprovacao/rejeicao de atestados
 * e a propagacao do efeito para os registros de presenca.
 *
 * <p>Esta e a unica ponte explicita entre os modulos {@code document}
 * e {@code academic} — toda alteracao em Attendance disparada por
 * atestado passa por aqui.</p>
 */
@Service
@RequiredArgsConstructor
public class AtestadoApprovalService {

    static final Set<DocumentCategory> ATESTADO_CATEGORIES = Set.of(
            DocumentCategory.ATESTADO_MEDICO,
            DocumentCategory.JUSTIFICATIVA_FALTA
    );

    private final UserDocumentRepository documentRepository;
    private final AttendanceRepository attendanceRepository;

    // ── Listagem para a secretaria ──────────────────────────────────────────

    public List<DocumentResponse> listPending() {
        return documentRepository
                .findByStatusAndCategoryInAndActiveTrueOrderByUploadedAtAsc(
                        DocumentStatus.PENDING, ATESTADO_CATEGORIES)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public List<DocumentResponse> listAll() {
        return documentRepository
                .findByCategoryInAndActiveTrueOrderByUploadedAtDesc(ATESTADO_CATEGORIES)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    // ── Aprovacao / reaprovacao ─────────────────────────────────────────────

    /**
     * Aprova o atestado para o periodo informado e justifica as faltas
     * do aluno cujas datas caem dentro do intervalo.
     *
     * <p>Operacao idempotente: se o documento ja estava APPROVED, as
     * justificativas anteriores deste documento sao revertidas antes
     * de aplicar o novo periodo (permite ao secretario corrigir datas).</p>
     */
    @Transactional
    public DocumentResponse approve(Long documentId, Long reviewerUserId,
                                    LocalDate start, LocalDate end) {
        UserDocument doc = findActiveOrThrow(documentId);
        assertIsAtestado(doc);

        // Reaprovacao: limpa estado anterior antes de aplicar novo periodo
        if (doc.getStatus() == DocumentStatus.APPROVED) {
            revertJustificationsOf(doc.getId());
        }

        doc.approve(reviewerUserId, start, end);
        documentRepository.save(doc);

        applyJustificationsFor(doc, start, end);
        return DocumentResponse.from(doc);
    }

    @Transactional
    public DocumentResponse reject(Long documentId, Long reviewerUserId, String reason) {
        UserDocument doc = findActiveOrThrow(documentId);
        assertIsAtestado(doc);

        // Defesa em profundidade: se por algum motivo havia justificativas vinculadas, reverte
        if (doc.getStatus() == DocumentStatus.APPROVED) {
            revertJustificationsOf(doc.getId());
        }

        doc.reject(reviewerUserId, reason);
        documentRepository.save(doc);
        return DocumentResponse.from(doc);
    }

    // ── Hooks chamados por outros servicos ──────────────────────────────────

    /**
     * Chamado pelo {@code DocumentService} quando o documento e excluido.
     * Reverte qualquer justificativa que tenha sido aplicada por ele.
     */
    @Transactional
    public void cancelOnDelete(UserDocument doc) {
        if (!ATESTADO_CATEGORIES.contains(doc.getCategory())) {
            return;
        }
        if (doc.getStatus() == DocumentStatus.APPROVED) {
            revertJustificationsOf(doc.getId());
        }
    }

    /**
     * Chamado pelo {@code AttendanceService} antes de gravar uma falta.
     * Se houver atestado APPROVED cobrindo a data, retorna o id dele
     * para a falta ja nascer como JUSTIFIED.
     *
     * @return id do documento que cobre, ou {@code null} se nao houver
     */
    public Long findCoveringApprovedAtestado(Long studentPersonId, LocalDate date) {
        List<UserDocument> hits = documentRepository.findApprovedCoveringDate(studentPersonId, date);
        return hits.isEmpty() ? null : hits.get(0).getId();
    }

    // ── Helpers privados ────────────────────────────────────────────────────

    private void applyJustificationsFor(UserDocument doc, LocalDate start, LocalDate end) {
        // Converte intervalo de datas para LocalDateTime: [start 00:00, end+1 00:00)
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<Attendance> absences = attendanceRepository.findAbsencesByStudentAndPeriod(
                doc.getOwnerPersonId(), startDateTime, endDateTime);

        for (Attendance a : absences) {
            a.applyStatus(AttendanceStatus.JUSTIFIED);
            a.setJustifiedByDocumentId(doc.getId());
            a.setObservation("Justificado pelo atestado #" + doc.getId());
        }
        attendanceRepository.saveAll(absences);
    }

    private void revertJustificationsOf(Long documentId) {
        List<Attendance> justified = attendanceRepository.findByJustifiedByDocumentId(documentId);
        for (Attendance a : justified) {
            a.applyStatus(AttendanceStatus.ABSENT);
            a.setJustifiedByDocumentId(null);
            a.setObservation(null);
        }
        attendanceRepository.saveAll(justified);
    }

    private void assertIsAtestado(UserDocument doc) {
        if (!ATESTADO_CATEGORIES.contains(doc.getCategory())) {
            throw new AtestadoOperationException(
                    "Documento " + doc.getId() + " nao e atestado nem justificativa de falta.");
        }
    }

    private UserDocument findActiveOrThrow(Long id) {
        return documentRepository.findById(id)
                .filter(UserDocument::isActive)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }
}
