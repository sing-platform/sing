package br.cadi.document.domain.exception;

import br.cadi.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentEmptyFileException extends BusinessException {

    public DocumentEmptyFileException() {
        super("O arquivo enviado está vazio", HttpStatus.BAD_REQUEST);
    }
}
