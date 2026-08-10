package br.cadi.document.domain.exception;

import br.cadi.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends BusinessException {

    public DocumentNotFoundException(Long id) {
        super("Documento não encontrado: id=" + id, HttpStatus.NOT_FOUND);
    }
}
