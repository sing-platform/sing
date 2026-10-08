package br.sing.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

// src/main/java/com/yourapp/exception/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //  Handles all your custom business exceptions
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business exception: {}", ex.getMessage());

        ApiError error = ApiError.of(ex.getStatus(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        log.warn("HTTP {}: {}", status.value(), message);
        ApiError error = ApiError.of(status, message, request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }




    //  Handles malformed JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ApiError error = ApiError.of(HttpStatus.BAD_REQUEST, "Malformed JSON request", request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }




    //  Handles wrong HTTP method (e.g., POST instead of GET)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        ApiError error = ApiError.of(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }




    //  Catch-all for unexpected errors (always keep this as a safety net)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiError error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(error);
    }




    @ExceptionHandler(ServerError.class)
    public ResponseEntity<ApiError> ServerError(
            ServerError ex, HttpServletRequest request) {
            ApiError error = ApiError.of(ex.status, ex.getMessage(), request.getRequestURI());
            return ResponseEntity.internalServerError().body(error);
    }




    @ExceptionHandler(UserCreationConflict.class)
    public ResponseEntity<ApiError> UserCreationConflict(
            UserCreationConflict ex, HttpServletRequest request
    ){
        ApiError error = ApiError.of(ex.status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.status).body(error);
    }




    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> EntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request
    ){
        ApiError error = ApiError.of(ex.status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.status).body(error);
    }





    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBindException(
            BindException ex, HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getFieldErrors()
                .stream()
                .map(e -> ApiError.FieldError.builder()
                        .field(e.getField())
                        .rejectedValue(e.getRejectedValue())
                        .message(e.getDefaultMessage())
                        .build())
                .toList();

        ApiError error = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .fields(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    



    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getAllValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors()
                        .stream()
                        .map(e -> ApiError.FieldError.builder()
                                .field(result.getMethodParameter().getParameterName())
                                .rejectedValue(result.getArgument())
                                .message(e.getDefaultMessage())
                                .build()))
                .toList();

        ApiError error = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more parameters are invalid")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .fields(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    //  @Validated em @PathVariable, @RequestParam, @RequestHeader
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(v -> ApiError.FieldError.builder()
                        // extrai só o nome do campo (sem o nome do método)
                        .field(extractFieldName(v.getPropertyPath().toString()))
                        .rejectedValue(v.getInvalidValue())
                        .message(v.getMessage())
                        .build())
                .toList();

        ApiError error = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more parameters are invalid")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .fields(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // Handles Bean Validation errors (@Valid, @NotNull, @Size, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> ApiError.FieldError.builder()
                        .field(e.getField())
                        .rejectedValue(e.getRejectedValue())
                        .message(e.getDefaultMessage())
                        .build())
                .toList();

        ApiError error = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .fields(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiError> handleValidationErrors(
//            MethodArgumentNotValidException ex, HttpServletRequest request) {
//
//        List<String> details = ex.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .map(e -> e.getField() + ": " + e.getDefaultMessage())
//                .toList();
//
//        ApiError apiError = ApiError.builder()
//                .status(HttpStatus.BAD_REQUEST.value())
//                .message("Validation failed")
//                .path(request.getRequestURI())
//                .details(details)
//                .build();
//
//        return ResponseEntity.badRequest().body(apiError);
//    }



    // utilitário: "createUser.name" → "name"
    private String extractFieldName(String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        return parts[parts.length - 1];
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(HttpStatus.FORBIDDEN, "Você não tem permissão para realizar esta ação.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}

