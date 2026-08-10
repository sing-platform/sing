package br.cadi.document.domain.exception;

import br.cadi.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentStorageException extends BusinessException {

    public DocumentStorageException(String operation, Throwable cause) {
        super("Falha no armazenamento durante operação '" + operation + "': " + cause.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
