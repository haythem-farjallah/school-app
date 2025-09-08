package com.example.school_management.commons.security;

/**
 * Custom exception for file security validation failures
 */
public class FileSecurityException extends RuntimeException {
    
    public FileSecurityException(String message) {
        super(message);
    }
    
    public FileSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
