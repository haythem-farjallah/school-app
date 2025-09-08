package com.example.school_management.feature.auth.controller;

import com.example.school_management.feature.auth.dto.UserDto;
import com.example.school_management.feature.auth.dto.UserProfileUpdateRequest;
import com.example.school_management.feature.auth.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public UserDto getMyProfile() {
        return service.getCurrentUserProfile();
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public UserDto updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return service.updateCurrentUserProfile(request);
    }
}
