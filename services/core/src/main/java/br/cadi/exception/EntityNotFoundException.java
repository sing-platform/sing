package br.cadi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EntityNotFoundException extends RuntimeException {
    public HttpStatus status =  HttpStatus.NOT_FOUND;
    public EntityNotFoundException(String message) {
        super(message);
    }
}
