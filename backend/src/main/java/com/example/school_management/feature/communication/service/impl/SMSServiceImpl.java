package com.example.school_management.feature.communication.service.impl;

import com.example.school_management.feature.communication.dto.*;
import com.example.school_management.feature.communication.repository.*;
import com.example.school_management.feature.communication.service.SMSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SMSServiceImpl implements SMSService {

    private final CommunicationNotificationRepository notificationRepository;
    private final CommunicationLogRepository communicationLogRepository;

    @Value("${app.sms.default-country-code:+1}")
    private String defaultCountryCode;

    @Value("${app.sms.cost.per-message:0.0075}")
    private double costPerMessage;

    // Phone number validation patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$" // E.164 format
    );

    // Opt-out storage (in production, this would be in database)
    private final Set<String> optedOutNumbers = new HashSet<>();

    @Override
    public SMSResponse sendSMS(SMSRequest smsRequest) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse sendTemplatedSMS(String templateName, String recipientPhone, Map<String, Object> variables) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public List<SMSResponse> sendBulkSMS(BulkSMSRequest bulkSMSRequest) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public List<SMSResponse> sendBulkTemplatedSMS(String templateName, List<String> recipientPhones, Map<String, Object> variables) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse scheduleSMS(SMSRequest smsRequest, LocalDateTime scheduledAt) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse sendEmergencyAlert(String recipientPhone, String alertMessage) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse sendAttendanceAlertSMS(String recipientPhone, String studentName, String alertType) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse sendGradeNotificationSMS(String recipientPhone, String studentName, String courseName, String grade) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse sendAssignmentReminderSMS(String recipientPhone, String assignmentTitle, LocalDateTime dueDate) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse sendPaymentReminderSMS(String recipientPhone, String studentName, Double amount, LocalDateTime dueDate) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse sendAnnouncementSMS(String recipientPhone, String title, String content) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String cleanPhone = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    @Override
    public String formatPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null) return null;
        
        String cleanPhone = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        
        if (cleanPhone.startsWith("+")) {
            return cleanPhone;
        }
        
        if (countryCode == null) {
            countryCode = defaultCountryCode;
        }
        
        if (!countryCode.startsWith("+")) {
            countryCode = "+" + countryCode;
        }
        
        return countryCode + cleanPhone;
    }

    @Override
    public String getSMSStatus(String messageId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public SMSResponse retryFailedSMS(Long notificationId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public Map<String, Object> getSMSAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("📊 Generating SMS analytics from {} to {}", startDate, endDate);

        Map<String, Object> analytics = new HashMap<>();

        // Get basic stats from notification repository
        List<Object[]> statusStats = notificationRepository.getNotificationStatusStats(startDate, endDate);
        Map<String, Long> statusMap = statusStats.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
        analytics.put("statusBreakdown", statusMap);

        // Get delivery stats from communication logs
        List<Object[]> deliveryStats = communicationLogRepository.getDeliveryStatusStats(startDate, endDate);
        Map<String, Long> deliveryMap = deliveryStats.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
        analytics.put("deliveryBreakdown", deliveryMap);

        // Get cost analysis
        Double totalCost = communicationLogRepository.getTotalCostByDateRange(startDate, endDate);
        analytics.put("totalCost", totalCost != null ? totalCost : 0.0);

        // Get performance metrics
        Double avgDeliveryTime = communicationLogRepository.getAverageDeliveryTimeInSeconds(startDate, endDate);
        analytics.put("averageDeliveryTimeSeconds", avgDeliveryTime != null ? avgDeliveryTime : 0.0);

        return analytics;
    }

    @Override
    public Map<String, Object> getAccountBalance() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public Double estimateSMSCost(String recipientPhone, String message) {
        if (message == null) return 0.0;
        
        int parts = 1;
        if (message.length() > 160) {
            parts = (int) Math.ceil(message.length() / 153.0);
        }
        
        return parts * costPerMessage;
    }

    @Override
    public List<Map<String, String>> getSupportedCountries() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public void optOutPhoneNumber(String phoneNumber) {
        String formattedPhone = formatPhoneNumber(phoneNumber, defaultCountryCode);
        optedOutNumbers.add(formattedPhone);
        log.info("📵 Phone number opted out: {}", maskPhoneNumber(formattedPhone));
    }

    @Override
    public boolean isPhoneNumberOptedOut(String phoneNumber) {
        String formattedPhone = formatPhoneNumber(phoneNumber, defaultCountryCode);
        return optedOutNumbers.contains(formattedPhone);
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
    }
}
