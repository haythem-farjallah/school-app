package com.example.school_management.commons.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Turns exceptions thrown by controllers into RFC 9457 problem details
 * (type, title, status, detail, instance).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ================================================================
     *  1) Validation errors  -> 400
     * ================================================================ */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return problem(HttpStatus.BAD_REQUEST, errorMsg, request);
    }

    /* ================================================================
     *  2) Custom ResourceNotFound  -> 404
     * ================================================================ */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /* 2‑b) Custom Conflict -> 409
     * ------------------------------ */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {

        log.warn("Conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /* ================================================================
     *  3) Malformed JSON  -> 400
     * ================================================================ */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleBadJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed JSON request: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON request", request);
    }

    /* ================================================================
     *  3-b) Request errors raised by Spring MVC itself -> 4xx
     *       Each exception carries its own status, detail and headers
     *       (e.g. Allow for 405). These are client errors, not logged.
     * ================================================================ */
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,     // 405
            HttpMediaTypeException.class,                     // 406, 415
            MissingServletRequestParameterException.class,    // 400
            MissingServletRequestPartException.class,         // 400
            MaxUploadSizeExceededException.class              // 413
    })
    public ResponseEntity<ProblemDetail> handleFrameworkRequestError(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse error = (ErrorResponse) ex;
        ProblemDetail body = error.getBody();
        body.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders()).body(body);
    }

    /* 3-c) No controller mapped to the path -> 404
     * ------------------------------------------------ */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoEndpoint(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        return problem(HttpStatus.NOT_FOUND, "No endpoint matches this request", request);
    }

    /* 3-d) Path variable / request parameter of the wrong type -> 400
     *      The rejected value is not echoed back.
     * ------------------------------------------------ */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        return problem(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", request);
    }

    /* ================================================================
     *  4) DB constraint violations (duplicate email, …) -> 409
     * ================================================================ */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String detail = ex.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation: {}", detail);

        /* Unique e-mail constraint ----------------------------------- */
        if (detail != null && detail.contains("users_email_key")) {
            return problem(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", request);
        }

        /* Other constraint errors ------------------------------------ */
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", request);
    }

    /* ================================================================
     *  5) Propagated ResponseStatusException – keep status & reason
     * ================================================================ */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        return problem(ex.getStatusCode(), ex.getReason(), request);
    }

    /* ================================================================
     *  6) Method security denial (@PreAuthorize) – 403
     * ================================================================ */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", request);
    }

    /* ================================================================
     *  7) Fallback – 500
     * ================================================================ */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception caught: ", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR", request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatusCode status,
            String detail,
            HttpServletRequest request) {

        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(body);
    }
}
