package br.cadi.document.domain.exception;

import br.cadi.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Lancada quando uma operacao do fluxo de aprovacao e invalida
 * (ex.: tentar aprovar um documento que nao e atestado/justificativa,
 * ou periodo com end &lt; start).
 */
public class AtestadoOperationException extends BusinessException {
    public AtestadoOperationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
