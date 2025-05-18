package com.example.school_management.commons.exceptions;

import com.example.school_management.commons.dtos.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ================================================================
     *  1) Validation errors  -> 400
     * ================================================================ */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return build(HttpStatus.BAD_REQUEST, errorMsg, request);
    }

    /* ================================================================
     *  2) Custom ResourceNotFound  -> 404
     * ================================================================ */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /* 2‑b) Custom Conflict -> 409
     * ------------------------------ */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex,
            WebRequest request) {

        log.warn("Conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /* ================================================================
     *  3) Malformed JSON  -> 400
     * ================================================================ */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleBadJson(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        log.warn("Malformed JSON request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", request);
    }

    /* ================================================================
     *  4) DB constraint violations (duplicate email, …) -> 409
     * ================================================================ */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrity(
            DataIntegrityViolationException ex,
            WebRequest request) {

        String detail = ex.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation: {}", detail);

        /* Unique e-mail constraint ----------------------------------- */
        if (detail != null && detail.contains("users_email_key")) {
            return build(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", request);
        }

        /* Other constraint errors ------------------------------------ */
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", request);
    }

    /* ================================================================
     *  5) Propagated ResponseStatusException – keep status & message
     * ================================================================ */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(
            ResponseStatusException ex,
            WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        return build(status, ex.getReason(), request);
    }

    /* ================================================================
     *  6) Fallback – 500
     * ================================================================ */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            WebRequest request) {

        log.error("Unhandled exception caught: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                request);
    }

    /* ================================================================
     *  Helper builder
     * ================================================================ */
    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            WebRequest request) {

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.name())
                .statusCode(status.value())
                .message(message)
                .path(request.getDescription(false))   // e.g. "uri=/api/…"
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
