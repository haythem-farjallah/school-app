package com.example.school_management.feature.operational.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * The admin feed has no School ownership, so every route is unavailable until a tenant-owned feed
 * exists. Routes fail before any service or repository access and never return or modify feed rows.
 */
@RestController
@RequestMapping("/api/v1/admin-feeds")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Feeds", description = "Unavailable until tenant-scoped feed ownership is implemented")
@SecurityRequirement(name = "bearerAuth")
public class AdminFeedController {

    @Operation(summary = "Unavailable: admin feed is not tenant scoped")
    @GetMapping({"", "/{id}", "/event-type/{eventType}", "/notification-type/{notificationType}",
            "/severity/{severity}", "/entity-type/{entityType}", "/triggered-by/{userId}",
            "/target-user/{userId}", "/unread", "/high-priority", "/date-range", "/recent", "/stats"})
    public ResponseEntity<Void> read() {
        throw unavailable();
    }

    @Operation(summary = "Unavailable: admin feed is not tenant scoped")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete() {
        throw unavailable();
    }

    @Operation(summary = "Unavailable: admin feed is not tenant scoped")
    @PatchMapping({"/{id}/read", "/read-all"})
    public ResponseEntity<Void> markAsRead() {
        throw unavailable();
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ADMIN_FEED_NOT_TENANT_SCOPED");
    }
}
