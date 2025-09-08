package com.example.school_management.feature.communication.repository;

import com.example.school_management.feature.communication.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommunicationNotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    // Find notifications by recipient
    List<Notification> findByRecipientIdAndRecipientType(Long recipientId, Notification.RecipientType recipientType);
    
    Page<Notification> findByRecipientIdAndRecipientType(Long recipientId, Notification.RecipientType recipientType, Pageable pageable);

    // Find unread notifications by recipient (using readAt field - null means unread)
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.readAt IS NULL")
    List<Notification> findUnreadByRecipient(@Param("recipientId") Long recipientId, @Param("recipientType") Notification.RecipientType recipientType);
    
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.readAt IS NULL")
    Page<Notification> findUnreadByRecipient(@Param("recipientId") Long recipientId, @Param("recipientType") Notification.RecipientType recipientType, Pageable pageable);

    // Find notifications by channel
    List<Notification> findByChannel(Notification.NotificationChannel channel);
    
    Page<Notification> findByChannel(Notification.NotificationChannel channel, Pageable pageable);

    // Find notifications by priority
    List<Notification> findByPriority(Notification.Priority priority);
    
    Page<Notification> findByPriority(Notification.Priority priority, Pageable pageable);

    // Find notifications by status
    List<Notification> findByStatus(Notification.NotificationStatus status);
    
    Page<Notification> findByStatus(Notification.NotificationStatus status, Pageable pageable);

    // Find notifications by date range
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    Page<Notification> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // Find notifications by recipient and date range
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findByRecipientAndDateRange(
            @Param("recipientId") Long recipientId, 
            @Param("recipientType") Notification.RecipientType recipientType,
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate
    );

    // Count unread notifications by recipient
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.readAt IS NULL")
    Long countUnreadByRecipient(@Param("recipientId") Long recipientId, @Param("recipientType") Notification.RecipientType recipientType);

    // Find notifications by multiple statuses
    @Query("SELECT n FROM Notification n WHERE n.status IN :statuses")
    List<Notification> findByStatusIn(@Param("statuses") List<Notification.NotificationStatus> statuses);
    
    @Query("SELECT n FROM Notification n WHERE n.status IN :statuses")
    Page<Notification> findByStatusIn(@Param("statuses") List<Notification.NotificationStatus> statuses, Pageable pageable);

    // Analytics queries
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.channel = :channel AND n.createdAt BETWEEN :startDate AND :endDate")
    Long countByChannelAndDateRange(@Param("channel") Notification.NotificationChannel channel, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") Notification.NotificationStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find recent notifications (last 24 hours)
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("since") LocalDateTime since);
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :since ORDER BY n.createdAt DESC")
    Page<Notification> findRecentNotifications(@Param("since") LocalDateTime since, Pageable pageable);

    // Get notification status statistics
    @Query("SELECT n.status as status, COUNT(n) as count FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate GROUP BY n.status")
    List<Object[]> getNotificationStatusStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}