package com.example.school_management.commons.configs;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class FileUploadConfig {

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // Set maximum file size to 100MB
        factory.setMaxFileSize(DataSize.ofMegabytes(100));
        // Set maximum request size to 100MB
        factory.setMaxRequestSize(DataSize.ofMegabytes(100));
        // Set file size threshold to 2MB (files larger than this will be written to disk)
        factory.setFileSizeThreshold(DataSize.ofMegabytes(2));
        return factory.createMultipartConfig();
    }
}

