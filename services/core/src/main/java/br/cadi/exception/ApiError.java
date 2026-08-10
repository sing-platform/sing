package br.cadi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

// src/main/java/com/yourapp/exception/ApiError.java
@Getter
@Builder
public class ApiError {

    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final String path;
    private List<String> details;
    @JsonInclude(JsonInclude.Include.NON_NULL) // só aparece quando houver erros de campo
    private List<FieldError> fields;

    public static ApiError of(HttpStatus status, String message, String path) {
        return ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final Object rejectedValue;
        private final String message;
    }

}