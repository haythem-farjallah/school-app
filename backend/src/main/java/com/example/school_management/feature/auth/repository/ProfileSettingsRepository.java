package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.ProfileSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileSettingsRepository extends JpaRepository<ProfileSettings, Long> { }
