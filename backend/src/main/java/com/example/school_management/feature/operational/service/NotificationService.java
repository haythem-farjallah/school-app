package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /* ─── CRUD ─────────────────────────────────────────── */
    NotificationDto get(Long id);
    void delete(Long id);

    /* ─── USER NOTIFICATIONS ───────────────────────────── */
    Page<NotificationDto> getMyNotifications(Pageable page, Boolean readStatus);
    NotificationDto markAsRead(Long id);
    void markAllAsRead();
    Integer getUnreadCount();
} 