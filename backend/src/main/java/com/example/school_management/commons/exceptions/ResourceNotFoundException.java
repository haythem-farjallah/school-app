package com.example.school_management.commons.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     * Uses HttpStatus.NOT_FOUND by default.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message and HTTP status.
     *
     * @param message the detail message
     * @param status  the HTTP status to be used in the error response
     */
    public ResourceNotFoundException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return the HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }
}

