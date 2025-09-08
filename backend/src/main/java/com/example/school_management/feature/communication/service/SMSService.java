package com.example.school_management.feature.communication.service;

import com.example.school_management.feature.communication.dto.SMSRequest;
import com.example.school_management.feature.communication.dto.BulkSMSRequest;
import com.example.school_management.feature.communication.dto.SMSResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SMSService {

    /**
     * Send a single SMS
     */
    SMSResponse sendSMS(SMSRequest smsRequest);

    /**
     * Send SMS using template
     */
    SMSResponse sendTemplatedSMS(String templateName, String recipientPhone, Map<String, Object> variables);

    /**
     * Send bulk SMS
     */
    List<SMSResponse> sendBulkSMS(BulkSMSRequest bulkSMSRequest);

    /**
     * Send bulk SMS using template
     */
    List<SMSResponse> sendBulkTemplatedSMS(String templateName, List<String> recipientPhones, Map<String, Object> variables);

    /**
     * Schedule SMS for later sending
     */
    SMSResponse scheduleSMS(SMSRequest smsRequest, LocalDateTime scheduledAt);

    /**
     * Send OTP SMS
     */
    SMSResponse sendOTP(String recipientPhone, String otp, int expiryMinutes);

    /**
     * Send emergency alert SMS
     */
    SMSResponse sendEmergencyAlert(String recipientPhone, String alertMessage);

    /**
     * Send attendance alert SMS
     */
    SMSResponse sendAttendanceAlertSMS(String recipientPhone, String studentName, String alertType);

    /**
     * Send grade notification SMS
     */
    SMSResponse sendGradeNotificationSMS(String recipientPhone, String studentName, String courseName, String grade);

    /**
     * Send assignment reminder SMS
     */
    SMSResponse sendAssignmentReminderSMS(String recipientPhone, String assignmentTitle, LocalDateTime dueDate);

    /**
     * Send payment reminder SMS
     */
    SMSResponse sendPaymentReminderSMS(String recipientPhone, String studentName, Double amount, LocalDateTime dueDate);

    /**
     * Send announcement SMS
     */
    SMSResponse sendAnnouncementSMS(String recipientPhone, String title, String content);

    /**
     * Validate phone number
     */
    boolean isValidPhoneNumber(String phoneNumber);

    /**
     * Format phone number to international format
     */
    String formatPhoneNumber(String phoneNumber, String countryCode);

    /**
     * Get SMS delivery status
     */
    String getSMSStatus(String messageId);

    /**
     * Retry failed SMS
     */
    SMSResponse retryFailedSMS(Long notificationId);

    /**
     * Get SMS analytics
     */
    Map<String, Object> getSMSAnalytics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get account balance (for Twilio or other providers)
     */
    Map<String, Object> getAccountBalance();

    /**
     * Estimate SMS cost
     */
    Double estimateSMSCost(String recipientPhone, String message);

    /**
     * Get supported countries
     */
    List<Map<String, String>> getSupportedCountries();

    /**
     * Opt-out phone number from SMS
     */
    void optOutPhoneNumber(String phoneNumber);

    /**
     * Check if phone number is opted out
     */
    boolean isPhoneNumberOptedOut(String phoneNumber);
}
