package com.example.school_management.commons.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
public class FileSecurityService {

    @Value("${app.file.upload.max-size:104857600}")
    private long maxFileSize;

    // Allowed MIME types for each resource type
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = Map.of(
        "DOCUMENT", Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
        ),
        "VIDEO", Set.of(
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "video/x-msvideo"
        ),
        "IMAGE", Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp"
        ),
        "AUDIO", Set.of(
            "audio/mpeg",
            "audio/wav",
            "audio/ogg",
            "audio/aac"
        )
    );

    // Dangerous file extensions that should never be allowed
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", "app", "deb", "pkg", "dmg",
        "sh", "bash", "ps1", "dll", "sys", "msi", "reg", "lnk", "inf"
    );

    // File signatures (magic bytes) for validation
    private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
        "PDF", new byte[]{0x25, 0x50, 0x44, 0x46}, // %PDF
        "JPEG", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
        "PNG", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        "MP4", new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, // ftyp
        "ZIP", new byte[]{0x50, 0x4B, 0x03, 0x04} // Used by Office documents
    );

    // Malicious signatures to detect
    private static final Map<String, byte[]> MALICIOUS_SIGNATURES = Map.of(
        "PE_EXECUTABLE", new byte[]{0x4D, 0x5A}, // MZ header
        "ELF_EXECUTABLE", new byte[]{0x7F, 0x45, 0x4C, 0x46}, // ELF header
        "JAVA_CLASS", new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE}
    );

    // File size limits per type (in bytes)
    private static final Map<String, Long> TYPE_SIZE_LIMITS = Map.of(
        "DOCUMENT", 50L * 1024 * 1024, // 50MB
        "VIDEO", 200L * 1024 * 1024,   // 200MB
        "IMAGE", 10L * 1024 * 1024,    // 10MB
        "AUDIO", 50L * 1024 * 1024     // 50MB
    );

    /**
     * Comprehensive file validation
     */
    public FileValidationResult validateFile(MultipartFile file, String resourceType) {
        log.debug("Validating file: {} (size: {}, type: {})", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            // 1. Basic file validation
            validateBasicProperties(file);

            // 2. Filename security validation
            String sanitizedFilename = validateAndSanitizeFilename(file.getOriginalFilename());

            // 3. File extension validation
            String extension = getFileExtension(sanitizedFilename);
            validateFileExtension(extension);

            // 4. MIME type validation
            validateMimeType(file, resourceType);

            // 5. File size validation by type
            validateFileSizeByType(file, resourceType);

            // 6. Content validation (magic bytes)
            validateFileContent(file);

            // 7. Generate file hash for integrity
            String fileHash = generateFileHash(file);

            log.info("File validation successful: {} (hash: {})", sanitizedFilename, fileHash);
            return FileValidationResult.success(sanitizedFilename, fileHash);

        } catch (FileSecurityException e) {
            log.warn("File validation failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            return FileValidationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file validation: {}", e.getMessage(), e);
            return FileValidationResult.failure("File validation failed");
        }
    }

    private void validateBasicProperties(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileSecurityException("File cannot be null or empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileSecurityException(String.format(
                "File size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                file.getSize(), maxFileSize));
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new FileSecurityException("Filename cannot be empty");
        }
    }

    private String validateAndSanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new FileSecurityException("Filename cannot be empty");
        }

        // Check for path traversal attempts
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new FileSecurityException("Filename contains invalid path characters");
        }

        // Check for null bytes
        if (originalFilename.contains("\0")) {
            throw new FileSecurityException("Filename contains null bytes");
        }

        // Sanitize filename
        String sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Ensure reasonable length
        if (sanitized.length() > 255) {
            String extension = getFileExtension(sanitized);
            String name = sanitized.substring(0, 250 - extension.length());
            sanitized = name + "." + extension;
        }

        if (sanitized.isEmpty()) {
            throw new FileSecurityException("Filename becomes empty after sanitization");
        }

        return sanitized;
    }

    private void validateFileExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            throw new FileSecurityException("File must have an extension");
        }

        String lowerExtension = extension.toLowerCase();

        // Check against dangerous extensions
        if (DANGEROUS_EXTENSIONS.contains(lowerExtension)) {
            throw new FileSecurityException("File extension '" + extension + "' is not allowed for security reasons");
        }

        // Check for double extensions
        if (extension.contains(".")) {
            throw new FileSecurityException("Multiple file extensions are not allowed");
        }
    }

    private void validateMimeType(MultipartFile file, String resourceType) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new FileSecurityException("File content type cannot be determined");
        }

        Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES.get(resourceType);
        if (allowedMimeTypes == null || !allowedMimeTypes.contains(contentType)) {
            throw new FileSecurityException(String.format(
                "File MIME type '%s' is not allowed for resource type '%s'", contentType, resourceType));
        }
    }

    private void validateFileSizeByType(MultipartFile file, String resourceType) {
        Long maxSizeForType = TYPE_SIZE_LIMITS.get(resourceType);
        if (maxSizeForType != null && file.getSize() > maxSizeForType) {
            throw new FileSecurityException(String.format(
                "File size (%d bytes) exceeds maximum allowed size for %s (%d bytes)",
                file.getSize(), resourceType, maxSizeForType));
        }
    }

    private void validateFileContent(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            
            // Check for malicious file signatures
            for (Map.Entry<String, byte[]> signature : MALICIOUS_SIGNATURES.entrySet()) {
                if (startsWithSignature(fileBytes, signature.getValue())) {
                    throw new FileSecurityException("File contains malicious signature: " + signature.getKey());
                }
            }

            // Validate file format matches MIME type
            validateContentFormat(fileBytes, file.getContentType());

        } catch (IOException e) {
            throw new FileSecurityException("Unable to read file content for validation");
        }
    }

    private void validateContentFormat(byte[] content, String mimeType) {
        // Validate PDF files
        if ("application/pdf".equals(mimeType)) {
            if (!startsWithSignature(content, FILE_SIGNATURES.get("PDF"))) {
                throw new FileSecurityException("File claims to be PDF but content doesn't match");
            }
        }
        
        // Validate JPEG files
        else if ("image/jpeg".equals(mimeType)) {
            if (!startsWithSignature(content, FILE_SIGNATURES.get("JPEG"))) {
                throw new FileSecurityException("File claims to be JPEG but content doesn't match");
            }
        }
        
        // Validate PNG files
        else if ("image/png".equals(mimeType)) {
            if (!startsWithSignature(content, FILE_SIGNATURES.get("PNG"))) {
                throw new FileSecurityException("File claims to be PNG but content doesn't match");
            }
        }

        // Validate Office documents (they are ZIP files)
        else if (mimeType.contains("officedocument")) {
            if (!startsWithSignature(content, FILE_SIGNATURES.get("ZIP"))) {
                throw new FileSecurityException("Office document format is invalid");
            }
        }
    }

    private boolean startsWithSignature(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String generateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Failed to generate file hash: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    private String getFileExtension(String filename) {
        return StringUtils.getFilenameExtension(filename);
    }

    /**
     * Result class for file validation
     */
    public static class FileValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String sanitizedFilename;
        private final String fileHash;

        private FileValidationResult(boolean valid, String errorMessage, String sanitizedFilename, String fileHash) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.sanitizedFilename = sanitizedFilename;
            this.fileHash = fileHash;
        }

        public static FileValidationResult success(String sanitizedFilename, String fileHash) {
            return new FileValidationResult(true, null, sanitizedFilename, fileHash);
        }

        public static FileValidationResult failure(String errorMessage) {
            return new FileValidationResult(false, errorMessage, null, null);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getSanitizedFilename() { return sanitizedFilename; }
        public String getFileHash() { return fileHash; }
    }
}
