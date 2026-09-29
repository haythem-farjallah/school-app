package com.example.school_management.commons.security;

import com.example.school_management.IntegrationTest;
import jakarta.servlet.MultipartConfigElement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The servlet container and FileSecurityService enforce the same upload limit.
 * The files only report their size, so no large upload is held in memory.
 */
@IntegrationTest
class UploadSizeLimitIntegrationTest {

    @Autowired
    MultipartConfigElement multipartConfig;

    @Autowired
    FileSecurityService fileSecurityService;

    @Test
    void servletContainerRejectsNothingTheValidatorAccepts() {
        assertThat(multipartConfig.getMaxRequestSize()).isEqualTo(multipartConfig.getMaxFileSize());
    }

    @Test
    void videoMayUseTheWholeLimit() {
        long limit = multipartConfig.getMaxFileSize();

        assertThat(fileSecurityService.validateFile(video(limit), "VIDEO").isValid()).isTrue();
    }

    @Test
    void fileAboveTheLimitIsRejected() {
        long limit = multipartConfig.getMaxFileSize();

        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(video(limit + 1), "VIDEO");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("File size (%d bytes) exceeds maximum allowed size (%d bytes)", limit + 1, limit);
    }

    private static MultipartFile video(long reportedSize) {
        return new MockMultipartFile("file", "lesson.mp4", "video/mp4", new byte[] {0}) {
            @Override
            public long getSize() {
                return reportedSize;
            }
        };
    }
}
