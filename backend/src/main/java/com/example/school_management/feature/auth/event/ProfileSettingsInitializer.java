package com.example.school_management.feature.auth.event;

import com.example.school_management.feature.auth.dto.UserCreatedEvent;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.ProfileSettings;
import com.example.school_management.feature.auth.repository.ProfileSettingsRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProfileSettingsInitializer {

    private final ProfileSettingsRepository settingsRepo;

    public ProfileSettingsInitializer(ProfileSettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    // run *after* the user has been saved in the same transaction
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent evt) {
        BaseUser user = evt.user();
        ProfileSettings s = new ProfileSettings();
        s.setUser(user);
        s.setLanguage("en");
        s.setTheme("light");
        s.setNotificationsEnabled(true);
        s.setDarkMode(false);
        settingsRepo.save(s);
    }
}
