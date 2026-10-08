package br.sing.exception;

import org.springframework.http.HttpStatus;

public class ServerError extends RuntimeException{

    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    public ServerError(String message){
        super(message);

    }
}
