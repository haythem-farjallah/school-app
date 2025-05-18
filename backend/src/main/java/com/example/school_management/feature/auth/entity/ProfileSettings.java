package com.example.school_management.feature.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "profile_settings")
public class ProfileSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String language;
    private String theme;
    private Boolean notificationsEnabled;
    private Boolean darkMode;

    @OneToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id")
    private BaseUser user;
}