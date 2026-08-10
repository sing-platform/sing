package br.cadi.document.api.dto.request;

import br.cadi.document.domain.enums.DocumentCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload de upload de documento.
 *
 * <p>Os campos {@code referenceDateStart} e {@code referenceDateEnd} sao
 * opcionais e usados apenas para atestados/justificativas: representam a
 * data ou intervalo que o aluno declara que o atestado cobre. O secretario
 * pode editar essas datas no momento da aprovacao.</p>
 */
public record DocumentUploadRequest(

        @NotNull
        DocumentCategory category,

        @Size(max = 500)
        String description,

        LocalDate referenceDateStart,

        LocalDate referenceDateEnd
) {
    /** Construtor de compatibilidade para uso interno (descricao + categoria). */
    public DocumentUploadRequest(DocumentCategory category, String description) {
        this(category, description, null, null);
    }
}
