package com.example.school_management.commons.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.service.CacheManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for cache management operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Cache Management", description = "Administrative endpoints for cache management")
@SecurityRequirement(name = "bearerAuth")
public class CacheManagementController {

    private final CacheManagementService cacheManagementService;

    @Operation(summary = "Clear all caches")
    @PostMapping("/clear-all")
    public ResponseEntity<ApiSuccessResponse<Void>> clearAllCaches() {
        log.info("Admin requested to clear all caches");
        cacheManagementService.clearAllCaches();
        return ResponseEntity.ok(new ApiSuccessResponse<>("All caches cleared successfully", null));
    }

    @Operation(summary = "Clear user-related caches")
    @PostMapping("/clear-users")
    public ResponseEntity<ApiSuccessResponse<Void>> clearUserCaches() {
        log.info("Admin requested to clear user caches");
        cacheManagementService.clearUserCaches();
        return ResponseEntity.ok(new ApiSuccessResponse<>("User caches cleared successfully", null));
    }

    @Operation(summary = "Clear listing caches")
    @PostMapping("/clear-listings")
    public ResponseEntity<ApiSuccessResponse<Void>> clearListingCaches() {
        log.info("Admin requested to clear listing caches");
        cacheManagementService.clearListingCaches();
        return ResponseEntity.ok(new ApiSuccessResponse<>("Listing caches cleared successfully", null));
    }

    @Operation(summary = "Clear specific cache")
    @PostMapping("/clear/{cacheName}")
    public ResponseEntity<ApiSuccessResponse<Void>> clearCache(@PathVariable String cacheName) {
        log.info("Admin requested to clear cache: {}", cacheName);
        cacheManagementService.clearCache(cacheName);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Cache '" + cacheName + "' cleared successfully", null));
    }

    @Operation(summary = "Clear specific cache entry")
    @PostMapping("/clear/{cacheName}/{key}")
    public ResponseEntity<ApiSuccessResponse<Void>> clearCacheEntry(
            @PathVariable String cacheName, 
            @PathVariable String key) {
        log.info("Admin requested to clear cache entry: {} -> {}", cacheName, key);
        cacheManagementService.clearCacheEntry(cacheName, key);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Cache entry cleared successfully", null));
    }

    @Operation(summary = "Get cache statistics")
    @GetMapping("/stats")
    public ResponseEntity<ApiSuccessResponse<Void>> getCacheStats() {
        log.info("Admin requested cache statistics");
        cacheManagementService.logCacheStats();
        return ResponseEntity.ok(new ApiSuccessResponse<>("Cache statistics logged", null));
    }
}
