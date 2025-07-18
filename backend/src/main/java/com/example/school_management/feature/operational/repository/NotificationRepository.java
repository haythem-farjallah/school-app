package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    
    // Find notifications by user ID and read status, ordered by creation date (most recent first)
    List<Notification> findTop5ByUserIdAndReadStatusOrderByCreatedAtDesc(Long userId, Boolean readStatus);
    
    // Find all notifications for a user with pagination
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Find unread notifications for a user
    List<Notification> findByUserIdAndReadStatusFalseOrderByCreatedAtDesc(Long userId);
    
    // Count unread notifications for a user
    long countByUserIdAndReadStatusFalse(Long userId);
    
    // Find notifications by type for a user
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, com.example.school_management.feature.operational.entity.enums.NotificationType type);
} 