package br.cadi.document.application;

import br.cadi.document.api.dto.request.DocumentUploadRequest;
import br.cadi.document.api.dto.response.DocumentResponse;
import br.cadi.document.domain.entities.UserDocument;
import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.document.domain.enums.DocumentScope;
import br.cadi.document.domain.exception.DocumentCategoryNotAllowedException;
import br.cadi.document.domain.exception.DocumentEmptyFileException;
import br.cadi.document.domain.exception.DocumentFileTooLargeException;
import br.cadi.document.domain.exception.DocumentNotFoundException;
import br.cadi.document.domain.exception.DocumentUnsupportedTypeException;
import br.cadi.document.infra.repositories.UserDocumentRepository;
import br.cadi.document.infra.storage.DocumentStorageService;
import br.cadi.exception.ForbiddenException;
import br.cadi.exception.ResourceNotFoundException;
import br.cadi.people.application.StudentGuardianService;
import br.cadi.people.infra.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    // ── Tika: detecção de MIME por magic bytes (evita MIME spoofing) ─────────
    private static final Tika TIKA = new Tika();

    private static final long MAX_FILE_SIZE_MB = 10;
    private static final long MAX_FILE_SIZE = MAX_FILE_SIZE_MB * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<DocumentCategory> SELF_UPLOAD_CATEGORIES = Set.of(
            DocumentCategory.ATESTADO_MEDICO,
            DocumentCategory.JUSTIFICATIVA_FALTA
    );
    private static final Set<DocumentCategory> THERAPEUTIC_CATEGORIES = Set.of(
            DocumentCategory.ANOTACAO_TERAPEUTICA
    );

    private final UserDocumentRepository repository;
    private final DocumentStorageService storageService;
    private final PersonRepository personRepository;
    private final StudentGuardianService studentGuardianService;
    private final AtestadoApprovalService atestadoApprovalService;

    /**
     * Wrapper retornado pelo download: stream + metadados para o controller
     * montar o Content-Disposition correto.
     */
    public record DocumentDownload(InputStream stream, String originalFileName, String mimeType) {}

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponse uploadForPerson(Long ownerPersonId, Long uploadedByUserId,
                                            MultipartFile file, DocumentUploadRequest request,
                                            DocumentScope scope) {
        // 1. Valida tamanho e MIME real (por magic bytes, não pelo header do cliente)
        String detectedMime = validateAndDetectMime(file);

        String storageKey = buildStorageKey(ownerPersonId, file.getOriginalFilename());

        // 2. Faz upload no Azure
        storageService.upload(file, storageKey);

        // 3. Persiste no banco; se falhar, remove o blob já enviado (ação compensatória)
        try {
            UserDocument doc = UserDocument.builder()
                    .ownerPersonId(ownerPersonId)
                    .fileName(storageKey)
                    .originalFileName(file.getOriginalFilename())
                    .mimeType(detectedMime)          // MIME detectado pelo Tika, não o do cliente
                    .fileSize(file.getSize())
                    .category(request.category())
                    .scope(scope)
                    .storageKey(storageKey)
                    .uploadedByUserId(uploadedByUserId)
                    .uploadedAt(Instant.now())
                    .description(request.description())
                    // Datas declaradas pelo aluno no upload (apenas atestados/justificativas).
                    // Servem como sugestao — o secretario pode editar ao aprovar.
                    .referenceDateStart(request.referenceDateStart())
                    .referenceDateEnd(request.referenceDateEnd())
                    .build();

            return DocumentResponse.from(repository.save(doc));

        } catch (Exception e) {
            // Ação compensatória: blob órfão evitado removendo o arquivo do Azure
            storageService.delete(storageKey);
            throw e;
        }
    }

    @Transactional
    public DocumentResponse uploadOwn(Long personId, Long userId,
                                      MultipartFile file, DocumentUploadRequest request) {
        if (!SELF_UPLOAD_CATEGORIES.contains(request.category())) {
            throw new DocumentCategoryNotAllowedException(request.category(), SELF_UPLOAD_CATEGORIES);
        }
        return uploadForPerson(personId, userId, file, request, DocumentScope.STUDENT_PERSONAL);
    }

    @Transactional
    public DocumentResponse uploadForStudentAsGuardian(Long studentId, Long guardianUserId,
                                                       MultipartFile file, DocumentUploadRequest request) {
        if (!SELF_UPLOAD_CATEGORIES.contains(request.category())) {
            throw new DocumentCategoryNotAllowedException(request.category(), SELF_UPLOAD_CATEGORIES);
        }
        Long guardianPersonId = resolvePersonIdFromUserId(guardianUserId);
        if (!studentGuardianService.isGuardianOf(guardianPersonId, studentId)) {
            throw new ForbiddenException("Acesso negado: você não é responsável por este aluno.");
        }
        return uploadForPerson(studentId, guardianUserId, file, request, DocumentScope.STUDENT_PERSONAL);
    }

    public List<DocumentResponse> listForStudentAsGuardian(Long studentId, Long guardianUserId) {
        Long guardianPersonId = resolvePersonIdFromUserId(guardianUserId);
        if (!studentGuardianService.isGuardianOf(guardianPersonId, studentId)) {
            throw new ForbiddenException("Acesso negado: você não é responsável por este aluno.");
        }
        return listByPerson(studentId);
    }

    @Transactional
    public DocumentResponse uploadTherapeutic(Long patientPersonId, Long psychologistUserId,
                                               MultipartFile file, DocumentUploadRequest request) {
        if (!THERAPEUTIC_CATEGORIES.contains(request.category())) {
            throw new DocumentCategoryNotAllowedException(request.category(), THERAPEUTIC_CATEGORIES);
        }
        return uploadForPerson(patientPersonId, psychologistUserId, file, request, DocumentScope.THERAPEUTIC_PRIVATE);
    }

    // ── Listagem ──────────────────────────────────────────────────────────────

    public List<DocumentResponse> listByPerson(Long ownerPersonId) {
        return repository.findByOwnerPersonIdAndActiveTrue(ownerPersonId)
                .stream()
                .filter(d -> d.getScope() != DocumentScope.THERAPEUTIC_PRIVATE)
                .map(DocumentResponse::from)
                .toList();
    }

    public List<DocumentResponse> listOwn(Long ownerPersonId) {
        return repository.findByOwnerPersonIdAndActiveTrue(ownerPersonId)
                .stream()
                .filter(d -> d.getScope() != DocumentScope.THERAPEUTIC_PRIVATE)
                .map(DocumentResponse::from)
                .toList();
    }

    public List<DocumentResponse> listTherapeutic(Long patientPersonId, Long psychologistUserId) {
        return repository.findByUploadedByUserIdAndScopeAndActiveTrue(
                        psychologistUserId, DocumentScope.THERAPEUTIC_PRIVATE)
                .stream()
                .filter(d -> d.getOwnerPersonId().equals(patientPersonId))
                .map(DocumentResponse::from)
                .toList();
    }

    // ── Download ──────────────────────────────────────────────────────────────

    /**
     * Retorna stream + metadados do arquivo.
     *
     * <p>Controle de acesso:
     * <ul>
     *   <li>THERAPEUTIC_PRIVATE — somente o psicólogo que fez o upload (comparado por user.id)</li>
     *   <li>demais escopos — dono do documento (comparado por person.id) ou admin/secretaria</li>
     * </ul>
     *
     * <p><strong>Correção de segurança:</strong> o parâmetro de ownership agora usa
     * {@code requestingPersonId} (person.id), resolvido via {@link #resolvePersonIdFromUserId},
     * eliminando a confusão anterior entre user.id e person.id na verificação de acesso.
     */
    public DocumentDownload download(Long documentId, Long requestingUserId, boolean isAdminOrSecretary) {
        UserDocument doc = findActiveOrThrow(documentId);

        // Resolve person.id somente para usuários não-admin (evita query desnecessária)
        Long requestingPersonId = isAdminOrSecretary
                ? null
                : resolvePersonIdFromUserId(requestingUserId);

        assertAccess(doc, requestingUserId, requestingPersonId, isAdminOrSecretary);

        InputStream stream = storageService.download(doc.getStorageKey());
        return new DocumentDownload(stream, doc.getOriginalFileName(), doc.getMimeType());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponse updateDescription(Long documentId, String description) {
        UserDocument doc = findActiveOrThrow(documentId);
        doc.setDescription(description);
        return DocumentResponse.from(repository.save(doc));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Soft delete: marca como inativo e remove o blob do Azure.
     *
     * <p><strong>Correção de segurança:</strong> o blob agora é efetivamente removido do
     * storage ao excluir (anteriormente o arquivo permanecia indefinidamente no Azure).
     * O controle de acesso usa {@code person.id} corretamente para verificar ownership.
     */
    @Transactional
    public void delete(Long documentId, Long requestingUserId, boolean isAdminOrSecretary) {
        UserDocument doc = findActiveOrThrow(documentId);

        Long requestingPersonId = isAdminOrSecretary
                ? null
                : resolvePersonIdFromUserId(requestingUserId);

        assertAccess(doc, requestingUserId, requestingPersonId, isAdminOrSecretary);

        // Se for atestado/justificativa aprovado, reverte as faltas justificadas antes de excluir
        atestadoApprovalService.cancelOnDelete(doc);

        // Remove o blob do Azure antes de marcar como inativo
        storageService.delete(doc.getStorageKey());
        doc.setActive(false);
        repository.save(doc);
    }

    // ── Helpers públicos ──────────────────────────────────────────────────────

    public Long resolvePersonIdFromUserId(Long userId) {
        return personRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Person", userId))
                .getId();
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Valida o arquivo e retorna o MIME type detectado por magic bytes (Apache Tika).
     *
     * <p><strong>Segurança:</strong> {@code MultipartFile.getContentType()} retorna o valor
     * enviado pelo cliente no header do multipart e pode ser facilmente falsificado.
     * A detecção via Tika lê os primeiros bytes do arquivo (magic bytes) e é
     * independente do que o cliente declarou.
     */
    private String validateAndDetectMime(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentEmptyFileException();
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentFileTooLargeException(MAX_FILE_SIZE_MB);
        }

        try {
            String detected = TIKA.detect(file.getBytes(), file.getOriginalFilename());
            if (!ALLOWED_MIME_TYPES.contains(detected)) {
                throw new DocumentUnsupportedTypeException(detected);
            }
            return detected;
        } catch (DocumentUnsupportedTypeException e) {
            throw e;
        } catch (IOException e) {
            throw new DocumentEmptyFileException();
        }
    }

    /**
     * Verifica se o usuário tem permissão de acesso ao documento.
     *
     * <p><strong>Correção de segurança (crítica):</strong> a assinatura agora recebe
     * {@code requestingUserId} (user.id, para comparar com {@code uploadedByUserId}) e
     * {@code requestingPersonId} (person.id, para comparar com {@code ownerPersonId})
     * separadamente. A versão anterior usava apenas {@code requestingUserId} (user.id)
     * em ambas as comparações, o que causava falha de acesso quando {@code person.id ≠ user.id},
     * e potencial acesso indevido quando IDs coincidiam entre usuários diferentes.
     *
     * @param requestingUserId   user.id do solicitante (comparado com uploadedByUserId)
     * @param requestingPersonId person.id do solicitante (comparado com ownerPersonId); null se admin
     */
    private void assertAccess(UserDocument doc,
                               Long requestingUserId,
                               Long requestingPersonId,
                               boolean isAdminOrSecretary) {
        // Anotações terapêuticas: só o psicólogo que enviou pode acessar (user.id correto aqui)
        if (doc.getScope() == DocumentScope.THERAPEUTIC_PRIVATE
                && !doc.getUploadedByUserId().equals(requestingUserId)) {
            throw new ForbiddenException("Acesso negado: anotação terapêutica pertence a outro psicólogo.");
        }
        // Demais documentos: admin/secretaria passa; outros precisam ser o dono (person.id)
        if (doc.getScope() != DocumentScope.THERAPEUTIC_PRIVATE
                && !isAdminOrSecretary
                && !doc.getOwnerPersonId().equals(requestingPersonId)) {
            throw new ForbiddenException("Acesso negado: documento pertence a outro usuário.");
        }
    }

    private UserDocument findActiveOrThrow(Long id) {
        return repository.findById(id)
                .filter(UserDocument::isActive)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    private String buildStorageKey(Long ownerPersonId, String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "documents/" + ownerPersonId + "/" + UUID.randomUUID() + ext;
    }
}
