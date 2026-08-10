package br.cadi.exception;

import org.springframework.http.HttpStatus;

public class UserCreationConflict extends RuntimeException {
    public HttpStatus status = HttpStatus.CONFLICT;
    public UserCreationConflict(String message) {
        super(message);
    }
}
