package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.AdminFeed;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminFeedRepository extends JpaRepository<AdminFeed, Long>, JpaSpecificationExecutor<AdminFeed> {

    @Query("SELECT af FROM AdminFeed af WHERE af.eventType = :eventType ORDER BY af.createdAt DESC")
    Page<AdminFeed> findByEventType(@Param("eventType") AuditEventType eventType, Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.notificationType = :notificationType ORDER BY af.createdAt DESC")
    Page<AdminFeed> findByNotificationType(@Param("notificationType") NotificationType notificationType, Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.severity = :severity ORDER BY af.createdAt DESC")
    Page<AdminFeed> findBySeverity(@Param("severity") String severity, Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.entityType = :entityType ORDER BY af.createdAt DESC")
    Page<AdminFeed> findByEntityType(@Param("entityType") String entityType, Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.triggeredBy.id = :userId ORDER BY af.createdAt DESC")
    Page<AdminFeed> findByTriggeredById(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.targetUser.id = :userId ORDER BY af.createdAt DESC")
    Page<AdminFeed> findByTargetUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.isRead = false ORDER BY af.createdAt DESC")
    Page<AdminFeed> findUnread(Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.severity IN ('ERROR', 'CRITICAL') ORDER BY af.createdAt DESC")
    Page<AdminFeed> findHighPriority(Pageable pageable);

    @Query("SELECT af FROM AdminFeed af WHERE af.createdAt BETWEEN :startDate AND :endDate ORDER BY af.createdAt DESC")
    Page<AdminFeed> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate, 
                                   Pageable pageable);

    @Query("SELECT af FROM AdminFeed af ORDER BY af.createdAt DESC LIMIT :limit")
    List<AdminFeed> findRecent(@Param("limit") int limit);

    @Query("SELECT COUNT(af) FROM AdminFeed af WHERE af.isRead = false")
    long countUnread();

    @Query("SELECT COUNT(af) FROM AdminFeed af WHERE af.eventType = :eventType")
    long countByEventType(@Param("eventType") AuditEventType eventType);

    @Query("SELECT COUNT(af) FROM AdminFeed af WHERE af.severity = :severity")
    long countBySeverity(@Param("severity") String severity);

    @Modifying
    @Query("UPDATE AdminFeed af SET af.isRead = true WHERE af.id = :id")
    void markAsRead(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AdminFeed af SET af.isRead = true")
    void markAllAsRead();
} 