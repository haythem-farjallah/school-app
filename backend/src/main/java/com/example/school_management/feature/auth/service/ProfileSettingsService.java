package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.ProfileSettingsDto;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.ProfileSettings;
import com.example.school_management.feature.auth.mapper.ProfileSettingsMapper;
import com.example.school_management.feature.auth.repository.ProfileSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileSettingsService {
    private final ProfileSettingsRepository settingsRepo;
    private final CustomUserDetailsService      userDetailsService;
    private final ProfileSettingsMapper         mapper;

    private BaseUser currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userDetailsService.findBaseUserByEmail(email);
    }

    @Transactional(readOnly = true)
    public ProfileSettingsDto getForCurrentUser() {
        BaseUser user = currentUser();
        ProfileSettings settings = user.getProfileSettings();
        if (settings == null) {
            settings = createDefaultFor(user);
        }
        return mapper.toDto(settings);
    }

    @Transactional
    public ProfileSettingsDto updateForCurrentUser(ProfileSettingsDto dto) {
        BaseUser user = currentUser();
        ProfileSettings settings = user.getProfileSettings();
        if (settings == null) {
            // user hasn’t got one yet → build & assign defaults
            settings = new ProfileSettings();
            settings.setUser(user);
            settings.setLanguage("en");
            settings.setTheme("light");
            settings.setNotificationsEnabled(true);
            settings.setDarkMode(false);
        }
        // now patch everything in dto onto the (possibly new) settings
        mapper.patch(dto, settings);
        settings = settingsRepo.save(settings);
        user.setProfileSettings(settings);
        return mapper.toDto(settings);
    }

    private ProfileSettings createDefaultFor(BaseUser user) {
        ProfileSettings def = new ProfileSettings();
        def.setUser(user);
        def.setLanguage("en");
        def.setTheme("light");
        def.setNotificationsEnabled(true);
        def.setDarkMode(false);
        def = settingsRepo.save(def);
        user.setProfileSettings(def);
        return def;
    }

}
