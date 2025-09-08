package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.academic.dto.CreateLearningResourceRequest;
import com.example.school_management.feature.academic.dto.LearningResourceDto;
import com.example.school_management.feature.academic.dto.UpdateLearningResourceRequest;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.service.LearningResourceService;
import com.example.school_management.feature.academic.service.impl.LearningResourceServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/learning-resources")
@RequiredArgsConstructor
@Tag(name = "Learning Resources", description = "Endpoints for managing learning resources")
@SecurityRequirement(name = "bearerAuth")
public class LearningResourceController {

    private final LearningResourceService service;
    private final LearningResourceServiceImpl serviceImpl;
    private final ObjectMapper objectMapper;
    
    @Value("${app.file.upload.path:uploads/learning-resources}")
    private String uploadPath;

    @Operation(summary = "Create a new learning resource")
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> create(
            @Valid @RequestBody CreateLearningResourceRequest request) {
        log.debug("POST /learning-resources {}", request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(request)));
    }

    @Operation(summary = "Upload a learning resource file (documents, videos, presentations)")
    @PostMapping("/upload")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("type") ResourceType type,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam(value = "isPublic", defaultValue = "true") Boolean isPublic,
            @RequestParam(value = "classIds", required = false) String classIdsJson,
            @RequestParam(value = "courseIds", required = false) String courseIdsJson,
            @RequestParam(value = "tags", required = false) String tagsJson) {
        
        log.debug("POST /learning-resources/upload with file: {} ({})", file.getOriginalFilename(), file.getSize());
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        // Parse JSON arrays
        Set<Long> classIds = parseJsonToLongSet(classIdsJson);
        Set<Long> courseIds = parseJsonToLongSet(courseIdsJson);
        
        // Create request object - Constructor order: title, description, url, type, thumbnailUrl, duration, teacherIds, classIds, courseIds
        // Note: isPublic has default value so not in constructor, teacherIds will be set by service
        CreateLearningResourceRequest request = new CreateLearningResourceRequest(
            title,                      // String title
            description,                // String description  
            "temp-url",                // String url (will be replaced by service)
            type,                      // ResourceType type
            thumbnailUrl,              // String thumbnailUrl
            duration,                  // Integer duration
            Collections.emptySet(),    // Set<Long> teacherIds (will be set by service)
            classIds,                  // Set<Long> classIds
            courseIds                  // Set<Long> courseIds
        );
        
        // Use the custom method that accepts isPublic parameter
        LearningResourceDto result = serviceImpl.uploadResourceWithVisibility(file, request, isPublic);
        log.info("Successfully uploaded resource: {} with ID: {}", result.getTitle(), result.getId());
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("File uploaded successfully", result));
    }

    @Operation(summary = "Download uploaded files (increments download count)")
    @GetMapping("/files/{filename}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename).normalize();
            log.debug("Attempting to serve file: {} from path: {}", filename, filePath.toAbsolutePath());
            Resource resource = new FileSystemResource(filePath.toFile());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found or not readable: {} at path: {}", filename, filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // Increment download count
            log.info("Incrementing download count for filename: {}", filename);
            service.incrementDownloadCount(filename);
            
            // Determine content type
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error serving file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Preview uploaded files (increments view count)")
    @GetMapping("/preview/{filename}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<Resource> previewFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename).normalize();
            log.debug("Attempting to preview file: {} from path: {}", filename, filePath.toAbsolutePath());
            Resource resource = new FileSystemResource(filePath.toFile());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found or not readable: {} at path: {}", filename, filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // Increment view count
            log.info("Incrementing view count for filename: {}", filename);
            service.incrementViewCount(filename);
            
            // Determine content type
            String contentType = determineContentType(filename);
            
            // For preview, always use inline disposition
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error previewing file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Stream video files for online viewing")
    @GetMapping("/stream/{filename}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<Resource> streamVideo(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename).normalize();
            log.debug("Attempting to stream file: {} from path: {}", filename, filePath.toAbsolutePath());
            Resource resource = new FileSystemResource(filePath.toFile());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Video file not found or not readable: {} at path: {}", filename, filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // Only allow streaming for video files
            if (!isVideoFile(filename)) {
                return ResponseEntity.badRequest().build();
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error streaming video: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Update an existing learning resource")
    @Parameter(name = "id", description = "ID of the resource to update", required = true)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningResourceRequest request) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, request)));
    }

    @Operation(summary = "Get learning resource details by ID")
    @Parameter(name = "id", description = "ID of the resource to retrieve", required = true)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @Operation(summary = "Delete a learning resource")
    @Parameter(name = "id", description = "ID of the resource to delete", required = true)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "List learning resources with pagination and filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<PageDto<LearningResourceDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by resource type") @RequestParam(required = false) ResourceType type,
            @Parameter(description = "Filter by teacher ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "Filter by class ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "Filter by course ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "Search by title or description") @RequestParam(required = false) String search) {

        if (type != null) {
            var dto = new PageDto<>(service.findByType(type, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (teacherId != null) {
            var dto = new PageDto<>(service.findByTeacherId(teacherId, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (classId != null) {
            var dto = new PageDto<>(service.findByClassId(classId, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (courseId != null) {
            var dto = new PageDto<>(service.findByCourseId(courseId, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (search != null && !search.trim().isEmpty()) {
            var dto = new PageDto<>(service.searchByTitleOrDescription(search, search, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else {
            var dto = new PageDto<>(service.list(PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        }
    }

    @Operation(summary = "Add target classes to a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @PostMapping("/{id}/classes")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> addTargetClasses(
            @PathVariable Long id,
            @RequestBody Set<Long> classIds) {
        service.addTargetClasses(id, classIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Remove target classes from a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @DeleteMapping("/{id}/classes")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> removeTargetClasses(
            @PathVariable Long id,
            @RequestBody Set<Long> classIds) {
        service.removeTargetClasses(id, classIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Add target courses to a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @PostMapping("/{id}/courses")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> addTargetCourses(
            @PathVariable Long id,
            @RequestBody Set<Long> courseIds) {
        service.addTargetCourses(id, courseIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Remove target courses from a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @DeleteMapping("/{id}/courses")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> removeTargetCourses(
            @PathVariable Long id,
            @RequestBody Set<Long> courseIds) {
        service.removeTargetCourses(id, courseIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Add teachers to a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @PostMapping("/{id}/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> addTeachers(
            @PathVariable Long id,
            @RequestBody Set<Long> teacherIds) {
        service.addTeachers(id, teacherIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Remove teachers from a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @DeleteMapping("/{id}/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> removeTeachers(
            @PathVariable Long id,
            @RequestBody Set<Long> teacherIds) {
        service.removeTeachers(id, teacherIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    // Helper methods
    private Set<Long> parseJsonToLongSet(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptySet();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Set<Long>>() {});
        } catch (IOException e) {
            log.warn("Failed to parse JSON array to Long set: {}", json, e);
            return Collections.emptySet();
        }
    }

    private String determineContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        
        switch (extension) {
            // Documents
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt": return "text/plain";
            
            // Videos
            case "mp4": return "video/mp4";
            case "avi": return "video/x-msvideo";
            case "mov": return "video/quicktime";
            case "wmv": return "video/x-ms-wmv";
            case "flv": return "video/x-flv";
            case "webm": return "video/webm";
            case "mkv": return "video/x-matroska";
            
            // Images
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "svg": return "image/svg+xml";
            case "webp": return "image/webp";
            
            // Audio
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "aac": return "audio/aac";
            case "flac": return "audio/flac";
            
            default: return "application/octet-stream";
        }
    }

    private boolean isInlineViewable(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return extension.matches("pdf|jpg|jpeg|png|gif|bmp|svg|webp|mp4|webm|mov");
    }

    private boolean isVideoFile(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return extension.matches("mp4|avi|mov|wmv|flv|webm|mkv");
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}