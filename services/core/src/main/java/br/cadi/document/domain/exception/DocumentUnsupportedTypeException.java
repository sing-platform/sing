package br.cadi.document.domain.exception;

import br.cadi.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentUnsupportedTypeException extends BusinessException {

    public DocumentUnsupportedTypeException(String mimeType) {
        super("Tipo de arquivo não permitido: " + mimeType + ". Use PDF, JPEG, PNG ou WEBP",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
