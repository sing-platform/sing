package br.cadi.document.domain.exception;

import br.cadi.exception.BusinessRuleException;

public class DocumentFileTooLargeException extends BusinessRuleException {

    public DocumentFileTooLargeException(long maxMb) {
        super("Arquivo excede o tamanho máximo permitido de " + maxMb + " MB");
    }
}
