package com.example.school_management.feature.communication.service;

import com.example.school_management.feature.communication.dto.EmailRequest;
import com.example.school_management.feature.communication.dto.BulkEmailRequest;
import com.example.school_management.feature.communication.dto.EmailResponse;

import java.util.List;
import java.util.Map;

public interface EmailService {

    /**
     * Send a single email
     */
    EmailResponse sendEmail(EmailRequest emailRequest);

    /**
     * Send email using template
     */
    EmailResponse sendTemplatedEmail(String templateName, String recipientEmail, Map<String, Object> variables);

    /**
     * Send bulk emails
     */
    List<EmailResponse> sendBulkEmails(BulkEmailRequest bulkEmailRequest);

    /**
     * Send bulk emails using template
     */
    List<EmailResponse> sendBulkTemplatedEmails(String templateName, List<String> recipientEmails, Map<String, Object> variables);

    /**
     * Schedule email for later sending
     */
    EmailResponse scheduleEmail(EmailRequest emailRequest, java.time.LocalDateTime scheduledAt);

    /**
     * Send welcome email to new user
     */
    EmailResponse sendWelcomeEmail(Long recipientId, String recipientEmail, String userName, String temporaryPassword);

    /**
     * Send password reset email
     */
    EmailResponse sendPasswordResetEmail(String recipientEmail, String resetToken);

    /**
     * Send grade notification email
     */
    EmailResponse sendGradeNotificationEmail(String recipientEmail, String studentName, String courseName, String grade);

    /**
     * Send assignment due reminder email
     */
    EmailResponse sendAssignmentReminderEmail(String recipientEmail, String assignmentTitle, java.time.LocalDateTime dueDate);

    /**
     * Send attendance alert email
     */
    EmailResponse sendAttendanceAlertEmail(String recipientEmail, String studentName, String alertType);

    /**
     * Send announcement email
     */
    EmailResponse sendAnnouncementEmail(String recipientEmail, String title, String content);

    /**
     * Validate email address
     */
    boolean isValidEmail(String email);

    /**
     * Get email delivery status
     */
    String getEmailStatus(String messageId);

    /**
     * Retry failed email
     */
    EmailResponse retryFailedEmail(Long notificationId);

    /**
     * Get email analytics
     */
    Map<String, Object> getEmailAnalytics(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}
