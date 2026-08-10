package br.cadi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadCredentialsException extends RuntimeException  {

    private HttpStatus status;

    public BadCredentialsException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    public BadCredentialsException(String message) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
    }

}
