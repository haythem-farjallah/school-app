package com.example.school_management.feature.auth.controller;

import com.example.school_management.feature.auth.dto.ProfileSettingsDto;
import com.example.school_management.feature.auth.service.ProfileSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/settings")
@RequiredArgsConstructor
public class ProfileSettingsController {

    private final ProfileSettingsService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ProfileSettingsDto getMySettings() {
        return service.getForCurrentUser();
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public ProfileSettingsDto updateMySettings(
            @Valid @RequestBody ProfileSettingsDto dto) {
        return service.updateForCurrentUser(dto);
    }
}
