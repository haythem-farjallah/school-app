package com.example.school_management.commons.exceptions;

/** Invalid request or domain input; internal invariants remain ordinary server errors. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
