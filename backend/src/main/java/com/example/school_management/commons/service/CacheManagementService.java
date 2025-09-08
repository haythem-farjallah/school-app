package com.example.school_management.commons.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service for managing cache operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheManagementService {

    private final CacheManager cacheManager;

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        log.info("Clearing all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            log.debug("Cleared cache: {}", cacheName);
        });
    }

    /**
     * Clear specific cache by name
     */
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
    }

    /**
     * Clear user-related caches
     */
    public void clearUserCaches() {
        log.info("Clearing user-related caches");
        clearCache("users");
        clearCache("students");
        clearCache("teachers");
        clearCache("staff");
        clearCache("admins");
        clearCache("auth");
    }

    /**
     * Clear listing caches
     */
    public void clearListingCaches() {
        log.info("Clearing listing caches");
        clearCache("classes");
        clearCache("courses");
        clearCache("timetables");
        clearCache("announcements");
        clearCache("resources");
        clearCache("grades");
    }

    /**
     * Clear cache entry by key
     */
    public void clearCacheEntry(String cacheName, String key) {
        log.info("Clearing cache entry: {} -> {}", cacheName, key);
        Objects.requireNonNull(cacheManager.getCache(cacheName)).evict(key);
    }

    /**
     * Get cache statistics
     */
    public void logCacheStats() {
        log.info("Cache Statistics:");
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                log.info("Cache: {} - Native Cache: {}", cacheName, cache.getNativeCache().getClass().getSimpleName());
            }
        });
    }
}
