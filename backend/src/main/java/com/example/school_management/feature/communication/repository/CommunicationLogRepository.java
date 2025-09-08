package com.example.school_management.feature.communication.repository;

import com.example.school_management.feature.communication.entity.CommunicationLog;
import com.example.school_management.feature.communication.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long>, JpaSpecificationExecutor<CommunicationLog> {

    // Find logs by notification
    List<CommunicationLog> findByNotificationId(Long notificationId);

    // Find logs by channel
    List<CommunicationLog> findByChannel(Notification.NotificationChannel channel);

    // Find logs by status
    List<CommunicationLog> findByStatus(CommunicationLog.LogStatus status);

    // Find logs by external message ID
    Optional<CommunicationLog> findByExternalMessageId(String externalMessageId);

    // Find logs by provider
    List<CommunicationLog> findByProvider(String provider);

    // Find logs by recipient email
    List<CommunicationLog> findByRecipientEmail(String recipientEmail);

    // Find logs by recipient phone
    List<CommunicationLog> findByRecipientPhone(String recipientPhone);

    // Find logs by date range
    List<CommunicationLog> findBySentAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Analytics queries
    @Query("SELECT cl.status, COUNT(cl) FROM CommunicationLog cl WHERE cl.sentAt BETWEEN :startDate AND :endDate GROUP BY cl.status")
    List<Object[]> getDeliveryStatusStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cl.channel, COUNT(cl) FROM CommunicationLog cl WHERE cl.sentAt BETWEEN :startDate AND :endDate GROUP BY cl.channel")
    List<Object[]> getChannelUsageStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cl.provider, COUNT(cl) FROM CommunicationLog cl WHERE cl.sentAt BETWEEN :startDate AND :endDate GROUP BY cl.provider")
    List<Object[]> getProviderUsageStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Cost analysis
    @Query("SELECT SUM(cl.cost) FROM CommunicationLog cl WHERE cl.sentAt BETWEEN :startDate AND :endDate")
    Double getTotalCostByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cl.channel, SUM(cl.cost) FROM CommunicationLog cl WHERE cl.sentAt BETWEEN :startDate AND :endDate GROUP BY cl.channel")
    List<Object[]> getCostByChannelAndDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Performance metrics
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, cl.sentAt, cl.deliveredAt)) FROM CommunicationLog cl WHERE cl.deliveredAt IS NOT NULL AND cl.sentAt BETWEEN :startDate AND :endDate")
    Double getAverageDeliveryTimeInSeconds(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cl.channel, AVG(TIMESTAMPDIFF(SECOND, cl.sentAt, cl.deliveredAt)) FROM CommunicationLog cl WHERE cl.deliveredAt IS NOT NULL AND cl.sentAt BETWEEN :startDate AND :endDate GROUP BY cl.channel")
    List<Object[]> getAverageDeliveryTimeByChannel(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Engagement metrics
    @Query("SELECT COUNT(cl) FROM CommunicationLog cl WHERE cl.openedAt IS NOT NULL AND cl.sentAt BETWEEN :startDate AND :endDate")
    Long getOpenedCount(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(cl) FROM CommunicationLog cl WHERE cl.clickedAt IS NOT NULL AND cl.sentAt BETWEEN :startDate AND :endDate")
    Long getClickedCount(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Error analysis
    @Query("SELECT cl.errorCode, COUNT(cl) FROM CommunicationLog cl WHERE cl.status = 'FAILED' AND cl.sentAt BETWEEN :startDate AND :endDate GROUP BY cl.errorCode")
    List<Object[]> getErrorCodeStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Cleanup old logs
    @Query("DELETE FROM CommunicationLog cl WHERE cl.sentAt < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}
