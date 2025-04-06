package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.BaseUser;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user receiving the notification
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BaseUser user;

    private String message;
    private Boolean readStatus;
    private LocalDateTime createdAt;
}