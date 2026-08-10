package br.cadi.document.domain.exception;

import br.cadi.document.domain.enums.DocumentCategory;
import br.cadi.exception.BusinessRuleException;

import java.util.Set;
import java.util.stream.Collectors;

public class DocumentCategoryNotAllowedException extends BusinessRuleException {

    public DocumentCategoryNotAllowedException(DocumentCategory attempted, Set<DocumentCategory> allowed) {
        super("Categoria '" + attempted + "' não é permitida neste endpoint. "
                + "Permitidas: " + allowed.stream().map(Enum::name).collect(Collectors.joining(", ")));
    }

    public DocumentCategoryNotAllowedException(String message) {
        super(message);
    }
}
