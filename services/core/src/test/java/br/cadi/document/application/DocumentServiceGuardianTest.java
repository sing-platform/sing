package br.cadi.document.application;

import br.cadi.document.api.dto.request.DocumentUploadRequest;
import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.document.infra.repositories.UserDocumentRepository;
import br.cadi.document.infra.storage.DocumentStorageService;
import br.cadi.exception.ForbiddenException;
import br.cadi.people.application.StudentGuardianService;
import br.cadi.people.domain.entities.Guardian;
import br.cadi.people.infra.repositories.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceGuardianTest {

    @Mock private UserDocumentRepository repository;
    @Mock private DocumentStorageService storageService;
    @Mock private PersonRepository personRepository;
    @Mock private StudentGuardianService studentGuardianService;
    @Mock private AtestadoApprovalService atestadoApprovalService;
    @Mock private MultipartFile file;

    @InjectMocks private DocumentService documentService;

    @Test
    void uploadForStudentAsGuardianLancaForbiddenQuandoNaoEhResponsavel() {
        when(personRepository.findByUserId(10L)).thenReturn(Optional.of(Guardian.builder().id(5L).build()));
        when(studentGuardianService.isGuardianOf(5L, 99L)).thenReturn(false);

        DocumentUploadRequest request = new DocumentUploadRequest(
                DocumentCategory.ATESTADO_MEDICO, "atestado", null, null);

        assertThrows(ForbiddenException.class, () ->
                documentService.uploadForStudentAsGuardian(99L, 10L, file, request));

        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void listForStudentAsGuardianLancaForbiddenQuandoNaoEhResponsavel() {
        when(personRepository.findByUserId(10L)).thenReturn(Optional.of(Guardian.builder().id(5L).build()));
        when(studentGuardianService.isGuardianOf(5L, 99L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                documentService.listForStudentAsGuardian(99L, 10L));
    }
}
