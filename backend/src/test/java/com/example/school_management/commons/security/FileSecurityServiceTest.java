package com.example.school_management.commons.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class FileSecurityServiceTest {

    private FileSecurityService fileSecurityService;

    @BeforeEach
    void setUp() {
        fileSecurityService = new FileSecurityService();
        ReflectionTestUtils.setField(fileSecurityService, "maxFileSize", 104857600L); // 100MB
    }

    @Test
    void testValidPdfFile() {
        // Create a mock PDF file with proper PDF signature
        byte[] pdfContent = "%PDF-1.4\n1 0 obj\n<<\n/Type /Catalog\n>>\nendobj\n".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.pdf", 
            "application/pdf", 
            pdfContent
        );

        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(file, "DOCUMENT");
        
        assertTrue(result.isValid());
        assertNotNull(result.getSanitizedFilename());
        assertNotNull(result.getFileHash());
    }

    @Test
    void testDangerousExecutableFile() {
        // Create a mock executable file with PE signature
        byte[] exeContent = new byte[]{0x4D, 0x5A, (byte)0x90, 0x00}; // MZ header
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "malware.exe", 
            "application/octet-stream", 
            exeContent
        );

        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(file, "DOCUMENT");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not allowed for security reasons"));
    }

    @Test
    void testMimeTypeSpoofing() {
        // Create a file that claims to be PDF but has wrong content
        byte[] fakeContent = "This is not a PDF file".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "fake.pdf", 
            "application/pdf", 
            fakeContent
        );

        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(file, "DOCUMENT");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("content doesn't match"));
    }

    @Test
    void testPathTraversalAttack() {
        byte[] content = "safe content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "../../../etc/passwd", 
            "text/plain", 
            content
        );

        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(file, "DOCUMENT");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("invalid path characters"));
    }

    @Test
    void testFileSizeLimit() {
        // Create a file that's too large for documents (over 50MB)
        byte[] largeContent = new byte[60 * 1024 * 1024]; // 60MB
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "large.pdf", 
            "application/pdf", 
            largeContent
        );

        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(file, "DOCUMENT");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum allowed size"));
    }

    @Test
    void testValidImageFile() {
        // Create a mock JPEG file with proper JPEG signature
        byte[] jpegContent = new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "image.jpg", 
            "image/jpeg", 
            jpegContent
        );

        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(file, "IMAGE");
        
        assertTrue(result.isValid());
        assertEquals("image.jpg", result.getSanitizedFilename());
    }
}
