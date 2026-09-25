package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.UserProfileDto;
import com.example.school_management.feature.auth.dto.UserProfileUpdateRequest;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Permission;
import com.example.school_management.feature.auth.entity.ProfileSettings;
import com.example.school_management.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    private BaseUser currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userDetailsService.findBaseUserByEmail(email);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile() {
        return toProfile(currentUser());
    }

    @Transactional
    public UserProfileDto updateCurrentUserProfile(UserProfileUpdateRequest request) {
        BaseUser user = currentUser();

        // Update only allowed fields: telephone and address
        user.setTelephone(request.getTelephone());
        user.setAddress(request.getAddress());

        user = userRepository.save(user);
        return toProfile(user);
    }

    private static UserProfileDto toProfile(BaseUser user) {
        ProfileSettings settings = user.getProfileSettings();
        Set<String> permissions = user.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return new UserProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getTelephone(),
                user.getAddress(),
                settings != null ? settings.getTheme() : null,
                settings != null ? settings.getLanguage() : null,
                permissions
        );
    }
}
