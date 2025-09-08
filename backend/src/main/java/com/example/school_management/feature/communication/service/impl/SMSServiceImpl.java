package com.example.school_management.feature.communication.service.impl;

import com.example.school_management.feature.communication.dto.*;
import com.example.school_management.feature.communication.entity.*;
import com.example.school_management.feature.communication.repository.*;
import com.example.school_management.feature.communication.service.SMSService;
import com.example.school_management.feature.communication.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SMSServiceImpl implements SMSService {

    private final CommunicationNotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final CommunicationLogRepository communicationLogRepository;
    private final NotificationTemplateService templateService;

    @Value("${app.sms.provider:twilio}")
    private String smsProvider;

    @Value("${app.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio.phone-number:}")
    private String twilioPhoneNumber;

    @Value("${app.sms.default-country-code:+1}")
    private String defaultCountryCode;

    @Value("${app.sms.async.enabled:true}")
    private boolean asyncEnabled;

    @Value("${app.sms.cost.per-message:0.0075}")
    private double costPerMessage;

    // Phone number validation patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$" // E.164 format
    );

    private static final Pattern US_PHONE_PATTERN = Pattern.compile(
            "^\\+?1?[2-9]\\d{2}[2-9]\\d{2}\\d{4}$"
    );

    // Opt-out storage (in production, this would be in database)
    private final Set<String> optedOutNumbers = new HashSet<>();

    @Override
    @Transactional
    public SMSResponse sendSMS(SMSRequest smsRequest) {
        log.info("📱 Sending SMS to: {}", maskPhoneNumber(smsRequest.getRecipientPhone()));

        try {
            // Validate phone number
            if (!isValidPhoneNumber(smsRequest.getRecipientPhone())) {
                return SMSResponse.failure(smsRequest.getRecipientPhone(), "Invalid phone number format", "INVALID_PHONE");
            }

            // Check opt-out status
            if (isPhoneNumberOptedOut(smsRequest.getRecipientPhone())) {
                return SMSResponse.failure(smsRequest.getRecipientPhone(), "Phone number has opted out", "OPTED_OUT");
            }

            // Format phone number
            String formattedPhone = formatPhoneNumber(smsRequest.getRecipientPhone(), smsRequest.getCountryCode());

            // Create notification record
            Notification notification = createNotificationRecord(smsRequest, formattedPhone);
            notification = notificationRepository.save(notification);

            // Send SMS via provider
            String messageId = sendViaTwilio(smsRequest, formattedPhone, notification.getId());

            // Update notification with message ID
            notification.setExternalId(messageId);
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Create communication log
            createCommunicationLog(notification, messageId, formattedPhone);

            log.info("✅ SMS sent successfully to: {} with messageId: {}", maskPhoneNumber(formattedPhone), messageId);
            
            SMSResponse response = SMSResponse.success(notification.getId(), messageId, formattedPhone);
            response.setMessage(smsRequest.getMessage());
            response.setMessageParts(smsRequest.getEstimatedParts());
            response.setCost(estimateSMSCost(formattedPhone, smsRequest.getMessage()));
            response.setCurrency("USD");
            response.setProvider(smsProvider);
            
            return response;

        } catch (Exception e) {
            log.error("❌ Failed to send SMS to: {}", maskPhoneNumber(smsRequest.getRecipientPhone()), e);
            return SMSResponse.failure(smsRequest.getRecipientPhone(), e.getMessage(), "SEND_ERROR");
        }
    }

    @Override
    @Transactional
    public SMSResponse sendTemplatedSMS(String templateName, String recipientPhone, Map<String, Object> variables) {
        log.info("📱 Sending templated SMS '{}' to: {}", templateName, maskPhoneNumber(recipientPhone));

        try {
            // Get template
            Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateNameAndTemplateTypeAndLanguage(
                    templateName, NotificationTemplate.TemplateType.SMS, "en"
            );

            if (templateOpt.isEmpty()) {
                return SMSResponse.failure(recipientPhone, "SMS template not found: " + templateName, "TEMPLATE_NOT_FOUND");
            }

            NotificationTemplate template = templateOpt.get();

            // Process template (SMS templates typically don't have subjects)
            String processedMessage = templateService.processTemplate(template.getContent(), variables);

            // Create SMS request
            SMSRequest smsRequest = SMSRequest.builder()
                    .recipientPhone(recipientPhone)
                    .message(processedMessage)
                    .templateName(templateName)
                    .templateVariables(variables)
                    .build();

            return sendSMS(smsRequest);

        } catch (Exception e) {
            log.error("❌ Failed to send templated SMS '{}' to: {}", templateName, maskPhoneNumber(recipientPhone), e);
            return SMSResponse.failure(recipientPhone, e.getMessage(), "TEMPLATE_ERROR");
        }
    }

    @Override
    @Transactional
    public List<SMSResponse> sendBulkSMS(BulkSMSRequest bulkSMSRequest) {
        log.info("📱 Sending bulk SMS to {} recipients", bulkSMSRequest.getTotalRecipients());

        List<SMSResponse> responses = new ArrayList<>();
        List<BulkSMSRequest.BulkSMSRecipient> recipients = bulkSMSRequest.getOptedInRecipients();

        // Validate total cost if specified
        if (bulkSMSRequest.getMaxTotalCost() != null) {
            double estimatedCost = bulkSMSRequest.getEstimatedTotalCost();
            if (estimatedCost > bulkSMSRequest.getMaxTotalCost()) {
                log.warn("Bulk SMS estimated cost (${}) exceeds maximum allowed (${}) - aborting", 
                        estimatedCost, bulkSMSRequest.getMaxTotalCost());
                return recipients.stream()
                        .map(r -> SMSResponse.failure(r.getPhone(), "Cost limit exceeded", "COST_LIMIT"))
                        .collect(Collectors.toList());
            }
        }

        // Process in batches
        int batchSize = bulkSMSRequest.getBatchSize();
        for (int i = 0; i < recipients.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, recipients.size());
            List<BulkSMSRequest.BulkSMSRecipient> batch = recipients.subList(i, endIndex);

            // Process batch
            List<SMSResponse> batchResponses = processSMSBatch(batch, bulkSMSRequest);
            responses.addAll(batchResponses);

            // Delay between batches
            if (endIndex < recipients.size() && bulkSMSRequest.getDelayBetweenBatches() > 0) {
                try {
                    Thread.sleep(bulkSMSRequest.getDelayBetweenBatches());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Bulk SMS processing interrupted");
                    break;
                }
            }
        }

        log.info("✅ Bulk SMS completed. Sent: {}, Failed: {}", 
                responses.stream().mapToLong(r -> r.getSuccess() ? 1 : 0).sum(),
                responses.stream().mapToLong(r -> r.getSuccess() ? 0 : 1).sum());

        return responses;
    }

    @Override
    public List<SMSResponse> sendBulkTemplatedSMS(String templateName, List<String> recipientPhones, Map<String, Object> variables) {
        log.info("📱 Sending bulk templated SMS '{}' to {} recipients", templateName, recipientPhones.size());

        List<BulkSMSRequest.BulkSMSRecipient> recipients = recipientPhones.stream()
                .map(phone -> BulkSMSRequest.BulkSMSRecipient.builder()
                        .phone(phone)
                        .personalizedVariables(variables)
                        .build())
                .collect(Collectors.toList());

        BulkSMSRequest bulkRequest = BulkSMSRequest.builder()
                .recipients(recipients)
                .templateName(templateName)
                .globalTemplateVariables(variables)
                .message("") // Will be filled from template
                .build();

        return sendBulkSMS(bulkRequest);
    }

    @Override
    @Transactional
    public SMSResponse scheduleSMS(SMSRequest smsRequest, LocalDateTime scheduledAt) {
        log.info("📅 Scheduling SMS to: {} for: {}", maskPhoneNumber(smsRequest.getRecipientPhone()), scheduledAt);

        try {
            // Create notification record with scheduled status
            String formattedPhone = formatPhoneNumber(smsRequest.getRecipientPhone(), smsRequest.getCountryCode());
            Notification notification = createNotificationRecord(smsRequest, formattedPhone);
            notification.setScheduledAt(scheduledAt);
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification = notificationRepository.save(notification);

            log.info("✅ SMS scheduled successfully for: {}", maskPhoneNumber(formattedPhone));
            return SMSResponse.scheduled(notification.getId(), formattedPhone, scheduledAt);

        } catch (Exception e) {
            log.error("❌ Failed to schedule SMS to: {}", maskPhoneNumber(smsRequest.getRecipientPhone()), e);
            return SMSResponse.failure(smsRequest.getRecipientPhone(), e.getMessage(), "SCHEDULE_ERROR");
        }
    }

    @Override
    public SMSResponse sendOTP(String recipientPhone, String otp, int expiryMinutes) {
        Map<String, Object> variables = Map.of(
                "otp", otp,
                "expiryMinutes", expiryMinutes,
                "schoolName", "School Management System"
        );
        return sendTemplatedSMS("otp-sms", recipientPhone, variables);
    }

    @Override
    public SMSResponse sendEmergencyAlert(String recipientPhone, String alertMessage) {
        SMSRequest request = SMSRequest.builder()
                .recipientPhone(recipientPhone)
                .message("🚨 EMERGENCY ALERT: " + alertMessage)
                .priority(Notification.Priority.URGENT)
                .smsType(SMSRequest.SMSType.EMERGENCY)
                .build();
        return sendSMS(request);
    }

    @Override
    public SMSResponse sendAttendanceAlertSMS(String recipientPhone, String studentName, String alertType) {
        Map<String, Object> variables = Map.of(
                "studentName", studentName,
                "alertType", alertType,
                "date", LocalDateTime.now().toString()
        );
        return sendTemplatedSMS("attendance-alert-sms", recipientPhone, variables);
    }

    @Override
    public SMSResponse sendGradeNotificationSMS(String recipientPhone, String studentName, String courseName, String grade) {
        Map<String, Object> variables = Map.of(
                "studentName", studentName,
                "courseName", courseName,
                "grade", grade
        );
        return sendTemplatedSMS("grade-notification-sms", recipientPhone, variables);
    }

    @Override
    public SMSResponse sendAssignmentReminderSMS(String recipientPhone, String assignmentTitle, LocalDateTime dueDate) {
        Map<String, Object> variables = Map.of(
                "assignmentTitle", assignmentTitle,
                "dueDate", dueDate.toString()
        );
        return sendTemplatedSMS("assignment-reminder-sms", recipientPhone, variables);
    }

    @Override
    public SMSResponse sendPaymentReminderSMS(String recipientPhone, String studentName, Double amount, LocalDateTime dueDate) {
        Map<String, Object> variables = Map.of(
                "studentName", studentName,
                "amount", String.format("%.2f", amount),
                "dueDate", dueDate.toString()
        );
        return sendTemplatedSMS("payment-reminder-sms", recipientPhone, variables);
    }

    @Override
    public SMSResponse sendAnnouncementSMS(String recipientPhone, String title, String content) {
        Map<String, Object> variables = Map.of(
                "title", title,
                "content", content
        );
        return sendTemplatedSMS("announcement-sms", recipientPhone, variables);
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
        // In a real implementation, this would query the SMS provider's API
        // For now, return a placeholder
        return "delivered";
    }

    @Override
    @Transactional
    public SMSResponse retryFailedSMS(Long notificationId) {
        log.info("🔄 Retrying failed SMS notification: {}", notificationId);

        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            return SMSResponse.failure("", "Notification not found", "NOT_FOUND");
        }

        Notification notification = notificationOpt.get();
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            return SMSResponse.failure("", "Maximum retry attempts exceeded", "MAX_RETRIES");
        }

        // Create SMS request from notification
        SMSRequest smsRequest = SMSRequest.builder()
                .recipientPhone("") // Would need to extract from notification metadata
                .message(notification.getContent())
                .build();

        // Increment retry count
        notification.setRetryCount(notification.getRetryCount() + 1);
        notificationRepository.save(notification);

        return sendSMS(smsRequest);
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
        // In a real implementation, this would query the SMS provider's API
        Map<String, Object> balance = new HashMap<>();
        balance.put("balance", 100.00);
        balance.put("currency", "USD");
        balance.put("provider", smsProvider);
        balance.put("lastUpdated", LocalDateTime.now());
        return balance;
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
        // Return list of supported countries with codes
        return List.of(
                Map.of("name", "United States", "code", "+1", "iso", "US"),
                Map.of("name", "Canada", "code", "+1", "iso", "CA"),
                Map.of("name", "United Kingdom", "code", "+44", "iso", "GB"),
                Map.of("name", "France", "code", "+33", "iso", "FR"),
                Map.of("name", "Germany", "code", "+49", "iso", "DE"),
                Map.of("name", "Australia", "code", "+61", "iso", "AU")
        );
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

    // Private helper methods

    private List<SMSResponse> processSMSBatch(List<BulkSMSRequest.BulkSMSRecipient> batch, BulkSMSRequest bulkRequest) {
        if (asyncEnabled) {
            // Process batch asynchronously
            List<CompletableFuture<SMSResponse>> futures = batch.stream()
                    .map(recipient -> CompletableFuture.supplyAsync(() -> processSMSRecipient(recipient, bulkRequest)))
                    .collect(Collectors.toList());

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } else {
            // Process batch synchronously
            return batch.stream()
                    .map(recipient -> processSMSRecipient(recipient, bulkRequest))
                    .collect(Collectors.toList());
        }
    }

    private SMSResponse processSMSRecipient(BulkSMSRequest.BulkSMSRecipient recipient, BulkSMSRequest bulkRequest) {
        try {
            SMSRequest smsRequest = SMSRequest.builder()
                    .recipientPhone(recipient.getPhone())
                    .recipientName(recipient.getName())
                    .message(bulkRequest.getMessage())
                    .templateName(bulkRequest.getTemplateName())
                    .templateVariables(mergeVariables(bulkRequest.getGlobalTemplateVariables(), recipient.getPersonalizedVariables()))
                    .priority(bulkRequest.getPriority())
                    .scheduledAt(recipient.getCustomScheduledAt() != null ? recipient.getCustomScheduledAt() : bulkRequest.getScheduledAt())
                    .category(bulkRequest.getCategory())
                    .countryCode(bulkRequest.getCountryCode())
                    .smsType(recipient.getCustomSmsType() != null ? recipient.getCustomSmsType() : bulkRequest.getSmsType())
                    .maxCost(recipient.getMaxCost() != null ? recipient.getMaxCost() : bulkRequest.getMaxCostPerMessage())
                    .build();

            if (bulkRequest.getTemplateName() != null) {
                return sendTemplatedSMS(bulkRequest.getTemplateName(), recipient.getPhone(), smsRequest.getTemplateVariables());
            } else {
                return sendSMS(smsRequest);
            }
        } catch (Exception e) {
            log.error("❌ Failed to process SMS recipient: {}", maskPhoneNumber(recipient.getPhone()), e);
            return SMSResponse.failure(recipient.getPhone(), e.getMessage(), "PROCESSING_ERROR");
        }
    }

    private Map<String, Object> mergeVariables(Map<String, Object> global, Map<String, Object> personal) {
        Map<String, Object> merged = new HashMap<>();
        if (global != null) merged.putAll(global);
        if (personal != null) merged.putAll(personal);
        return merged;
    }

    private Notification createNotificationRecord(SMSRequest smsRequest, String formattedPhone) {
        return Notification.builder()
                .recipientType(Notification.RecipientType.STUDENT) // Default, should be determined from context
                .notificationType(mapSMSTypeToNotificationType(smsRequest.getSmsType()))
                .channel(Notification.NotificationChannel.SMS)
                .title("SMS: " + smsRequest.getMessage().substring(0, Math.min(50, smsRequest.getMessage().length())))
                .content(smsRequest.getMessage())
                .status(Notification.NotificationStatus.PENDING)
                .priority(smsRequest.getPriority())
                .scheduledAt(smsRequest.getScheduledAt())
                .retryCount(0)
                .maxRetries(smsRequest.getMaxRetries())
                .build();
    }

    private Notification.NotificationType mapSMSTypeToNotificationType(SMSRequest.SMSType smsType) {
        return switch (smsType) {
            case EMERGENCY -> Notification.NotificationType.SYSTEM_ALERT;
            case REMINDER -> Notification.NotificationType.ASSIGNMENT_DUE;
            case NOTIFICATION -> Notification.NotificationType.ANNOUNCEMENT;
            case PROMOTIONAL -> Notification.NotificationType.ANNOUNCEMENT;
            case TRANSACTIONAL -> Notification.NotificationType.CUSTOM;
        };
    }

    private String sendViaTwilio(SMSRequest smsRequest, String formattedPhone, Long notificationId) {
        // In a real implementation, this would use the Twilio SDK
        // For now, simulate sending and return a mock message ID
        log.info("📱 Simulating Twilio SMS send to: {}", maskPhoneNumber(formattedPhone));
        
        // Simulate API call delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private void createCommunicationLog(Notification notification, String messageId, String recipientPhone) {
        CommunicationLog log = CommunicationLog.builder()
                .notificationId(notification.getId())
                .channel(Notification.NotificationChannel.SMS)
                .recipientPhone(recipientPhone)
                .status(CommunicationLog.LogStatus.SENT)
                .sentAt(LocalDateTime.now())
                .externalMessageId(messageId)
                .provider(smsProvider)
                .cost(costPerMessage)
                .retryCount(0)
                .build();

        communicationLogRepository.save(log);
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
    }
}
