package com.example.school_management.feature.communication.service.impl;

import com.example.school_management.feature.communication.dto.*;
import com.example.school_management.feature.communication.entity.*;
import com.example.school_management.feature.communication.repository.*;
import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.communication.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final CommunicationNotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final CommunicationLogRepository communicationLogRepository;
    private final NotificationTemplateService templateService;

    @Value("${app.email.from:noreply@schoolmanagement.com}")
    private String defaultFromEmail;

    @Value("${app.email.from-name:School Management System}")
    private String defaultFromName;

    @Value("${app.email.tracking.enabled:true}")
    private boolean trackingEnabled;

    @Value("${app.email.async.enabled:true}")
    private boolean asyncEnabled;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Override
    @Transactional
    public EmailResponse sendEmail(EmailRequest emailRequest) {
        return sendEmailWithRecipientId(emailRequest, null);
    }

    @Transactional
    public EmailResponse sendEmailWithRecipientId(EmailRequest emailRequest, Long recipientId) {
        log.info("📧 Sending email to: {} (recipientId: {})", emailRequest.getRecipientEmail(), recipientId);

        try {
            // Validate email
            if (!isValidEmail(emailRequest.getRecipientEmail())) {
                return EmailResponse.failure(emailRequest.getRecipientEmail(), "Invalid email address");
            }

            // Create notification record
            Notification notification = createNotificationRecord(emailRequest, recipientId);
            notification = notificationRepository.save(notification);

            // Send email
            String messageId = sendMimeMessage(emailRequest, notification.getId());

            // Update notification with message ID
            notification.setExternalId(messageId);
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Create communication log
            createCommunicationLog(notification, messageId, emailRequest.getRecipientEmail());

            log.info("✅ Email sent successfully to: {} with messageId: {}", emailRequest.getRecipientEmail(), messageId);
            return EmailResponse.success(notification.getId(), messageId, emailRequest.getRecipientEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send email to: {}", emailRequest.getRecipientEmail(), e);
            return EmailResponse.failure(emailRequest.getRecipientEmail(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public EmailResponse sendTemplatedEmail(String templateName, String recipientEmail, Map<String, Object> variables) {
        return sendTemplatedEmailWithRecipientId(templateName, null, recipientEmail, variables);
    }

    @Transactional
    public EmailResponse sendTemplatedEmailWithRecipientId(String templateName, Long recipientId, String recipientEmail, Map<String, Object> variables) {
        log.info("📧 Sending templated email '{}' to: {} (recipientId: {})", templateName, recipientEmail, recipientId);

        try {
            // Get template
            Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateNameAndTemplateTypeAndLanguage(
                    templateName, NotificationTemplate.TemplateType.EMAIL, "en"
            );

            if (templateOpt.isEmpty()) {
                return EmailResponse.failure(recipientEmail, "Email template not found: " + templateName);
            }

            NotificationTemplate template = templateOpt.get();

            // Process template
            String processedSubject = templateService.processTemplate(template.getSubject(), variables);
            String processedContent = templateService.processTemplate(template.getContent(), variables);

            // Create email request
            EmailRequest emailRequest = EmailRequest.builder()
                    .recipientEmail(recipientEmail)
                    .subject(processedSubject)
                    .content(processedContent)
                    .templateName(templateName)
                    .templateVariables(variables)
                    .isHtml(true)
                    .build();

            return sendEmailWithRecipientId(emailRequest, recipientId);

        } catch (Exception e) {
            log.error("❌ Failed to send templated email '{}' to: {}", templateName, recipientEmail, e);
            return EmailResponse.failure(recipientEmail, e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<EmailResponse> sendBulkEmails(BulkEmailRequest bulkEmailRequest) {
        log.info("📧 Sending bulk emails to {} recipients", bulkEmailRequest.getRecipients().size());

        List<EmailResponse> responses = new ArrayList<>();
        List<BulkEmailRequest.BulkEmailRecipient> recipients = bulkEmailRequest.getRecipients();

        // Process in batches
        int batchSize = bulkEmailRequest.getBatchSize();
        for (int i = 0; i < recipients.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, recipients.size());
            List<BulkEmailRequest.BulkEmailRecipient> batch = recipients.subList(i, endIndex);

            // Process batch
            List<EmailResponse> batchResponses = processBatch(batch, bulkEmailRequest);
            responses.addAll(batchResponses);

            // Delay between batches
            if (endIndex < recipients.size() && bulkEmailRequest.getDelayBetweenBatches() > 0) {
                try {
                    Thread.sleep(bulkEmailRequest.getDelayBetweenBatches());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Batch processing interrupted");
                    break;
                }
            }
        }

        log.info("✅ Bulk email completed. Sent: {}, Failed: {}", 
                responses.stream().mapToLong(r -> r.getSuccess() ? 1 : 0).sum(),
                responses.stream().mapToLong(r -> r.getSuccess() ? 0 : 1).sum());

        return responses;
    }

    @Override
    public List<EmailResponse> sendBulkTemplatedEmails(String templateName, List<String> recipientEmails, Map<String, Object> variables) {
        log.info("📧 Sending bulk templated emails '{}' to {} recipients", templateName, recipientEmails.size());

        List<BulkEmailRequest.BulkEmailRecipient> recipients = recipientEmails.stream()
                .map(email -> BulkEmailRequest.BulkEmailRecipient.builder()
                        .email(email)
                        .personalizedVariables(variables)
                        .build())
                .collect(Collectors.toList());

        BulkEmailRequest bulkRequest = BulkEmailRequest.builder()
                .recipients(recipients)
                .templateName(templateName)
                .globalTemplateVariables(variables)
                .subject("") // Will be filled from template
                .content("") // Will be filled from template
                .build();

        return sendBulkEmails(bulkRequest);
    }

    @Override
    @Transactional
    public EmailResponse scheduleEmail(EmailRequest emailRequest, LocalDateTime scheduledAt) {
        log.info("📅 Scheduling email to: {} for: {}", emailRequest.getRecipientEmail(), scheduledAt);

        try {
            // Create notification record with scheduled status
            Notification notification = createNotificationRecord(emailRequest);
            notification.setScheduledAt(scheduledAt);
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification = notificationRepository.save(notification);

            log.info("✅ Email scheduled successfully for: {}", emailRequest.getRecipientEmail());
            return EmailResponse.scheduled(notification.getId(), emailRequest.getRecipientEmail(), scheduledAt);

        } catch (Exception e) {
            log.error("❌ Failed to schedule email to: {}", emailRequest.getRecipientEmail(), e);
            return EmailResponse.failure(emailRequest.getRecipientEmail(), e.getMessage());
        }
    }

    @Override
    public EmailResponse sendWelcomeEmail(Long recipientId, String recipientEmail, String userName, String temporaryPassword) {
        Map<String, Object> variables = Map.of(
                "userName", userName,
                "temporaryPassword", temporaryPassword,
                "loginUrl", "https://schoolmanagement.com/login",
                "schoolName", "Our School",
                "year", String.valueOf(java.time.Year.now().getValue())
        );
        return sendTemplatedEmailWithRecipientId("welcome-email", recipientId, recipientEmail, variables);
    }

    @Override
    public EmailResponse sendPasswordResetEmail(String recipientEmail, String resetToken) {
        Map<String, Object> variables = Map.of(
                "resetToken", resetToken,
                "resetUrl", "https://schoolmanagement.com/reset-password?token=" + resetToken,
                "expiryTime", "24 hours"
        );
        return sendTemplatedEmail("password-reset", recipientEmail, variables);
    }

    @Override
    public EmailResponse sendGradeNotificationEmail(String recipientEmail, String studentName, String courseName, String grade) {
        Map<String, Object> variables = Map.of(
                "studentName", studentName,
                "courseName", courseName,
                "grade", grade,
                "date", LocalDateTime.now().toString()
        );
        return sendTemplatedEmail("grade-notification", recipientEmail, variables);
    }

    @Override
    public EmailResponse sendAssignmentReminderEmail(String recipientEmail, String assignmentTitle, LocalDateTime dueDate) {
        Map<String, Object> variables = Map.of(
                "assignmentTitle", assignmentTitle,
                "dueDate", dueDate.toString(),
                "portalUrl", "https://schoolmanagement.com/assignments"
        );
        return sendTemplatedEmail("assignment-reminder", recipientEmail, variables);
    }

    @Override
    public EmailResponse sendAttendanceAlertEmail(String recipientEmail, String studentName, String alertType) {
        Map<String, Object> variables = Map.of(
                "studentName", studentName,
                "alertType", alertType,
                "date", LocalDateTime.now().toString()
        );
        return sendTemplatedEmail("attendance-alert", recipientEmail, variables);
    }

    @Override
    public EmailResponse sendAnnouncementEmail(String recipientEmail, String title, String content) {
        Map<String, Object> variables = Map.of(
                "title", title,
                "content", content,
                "date", LocalDateTime.now().toString()
        );
        return sendTemplatedEmail("announcement", recipientEmail, variables);
    }

    @Override
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    public String getEmailStatus(String messageId) {
        // This would integrate with email provider's API to get delivery status
        // For now, return a placeholder
        return "DELIVERED";
    }

    @Override
    @Transactional
    public EmailResponse retryFailedEmail(Long notificationId) {
        log.info("🔄 Retrying failed email notification: {}", notificationId);

        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            return EmailResponse.failure("", "Notification not found");
        }

        Notification notification = notificationOpt.get();
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            return EmailResponse.failure("", "Maximum retry attempts exceeded");
        }

        // Create email request from notification
        EmailRequest emailRequest = EmailRequest.builder()
                .recipientEmail("") // Would need to extract from notification
                .subject(notification.getTitle())
                .content(notification.getContent())
                .build();

        // Increment retry count
        notification.setRetryCount(notification.getRetryCount() + 1);
        notificationRepository.save(notification);

        return sendEmail(emailRequest);
    }

    @Override
    public Map<String, Object> getEmailAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("📊 Generating email analytics from {} to {}", startDate, endDate);

        Map<String, Object> analytics = new HashMap<>();

        // Get basic stats
        List<Object[]> statusStats = notificationRepository.getNotificationStatusStats(startDate, endDate);
        Map<String, Long> statusMap = statusStats.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
        analytics.put("statusBreakdown", statusMap);

        // Get delivery stats
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

        // Get engagement metrics
        Long openedCount = communicationLogRepository.getOpenedCount(startDate, endDate);
        Long clickedCount = communicationLogRepository.getClickedCount(startDate, endDate);
        analytics.put("openedCount", openedCount);
        analytics.put("clickedCount", clickedCount);

        // Get performance metrics
        Double avgDeliveryTime = communicationLogRepository.getAverageDeliveryTimeInSeconds(startDate, endDate);
        analytics.put("averageDeliveryTimeSeconds", avgDeliveryTime != null ? avgDeliveryTime : 0.0);

        return analytics;
    }

    // Private helper methods

    private List<EmailResponse> processBatch(List<BulkEmailRequest.BulkEmailRecipient> batch, BulkEmailRequest bulkRequest) {
        if (asyncEnabled) {
            // Process batch asynchronously
            List<CompletableFuture<EmailResponse>> futures = batch.stream()
                    .map(recipient -> CompletableFuture.supplyAsync(() -> processRecipient(recipient, bulkRequest)))
                    .collect(Collectors.toList());

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } else {
            // Process batch synchronously
            return batch.stream()
                    .map(recipient -> processRecipient(recipient, bulkRequest))
                    .collect(Collectors.toList());
        }
    }

    private EmailResponse processRecipient(BulkEmailRequest.BulkEmailRecipient recipient, BulkEmailRequest bulkRequest) {
        try {
            EmailRequest emailRequest = EmailRequest.builder()
                    .recipientEmail(recipient.getEmail())
                    .recipientName(recipient.getName())
                    .subject(bulkRequest.getSubject())
                    .content(bulkRequest.getContent())
                    .templateName(bulkRequest.getTemplateName())
                    .templateVariables(mergeVariables(bulkRequest.getGlobalTemplateVariables(), recipient.getPersonalizedVariables()))
                    .isHtml(bulkRequest.getIsHtml())
                    .priority(bulkRequest.getPriority())
                    .scheduledAt(bulkRequest.getScheduledAt())
                    .category(bulkRequest.getCategory())
                    .enableTracking(bulkRequest.getEnableTracking())
                    .build();

            if (bulkRequest.getTemplateName() != null) {
                return sendTemplatedEmail(bulkRequest.getTemplateName(), recipient.getEmail(), emailRequest.getTemplateVariables());
            } else {
                return sendEmail(emailRequest);
            }
        } catch (Exception e) {
            log.error("❌ Failed to process recipient: {}", recipient.getEmail(), e);
            return EmailResponse.failure(recipient.getEmail(), e.getMessage());
        }
    }

    private Map<String, Object> mergeVariables(Map<String, Object> global, Map<String, Object> personal) {
        Map<String, Object> merged = new HashMap<>();
        if (global != null) merged.putAll(global);
        if (personal != null) merged.putAll(personal);
        return merged;
    }

    private Notification createNotificationRecord(EmailRequest emailRequest) {
        return createNotificationRecord(emailRequest, null);
    }

    private Notification createNotificationRecord(EmailRequest emailRequest, Long recipientId) {
        return Notification.builder()
                .recipientId(recipientId)
                .recipientType(Notification.RecipientType.STUDENT) // Default, should be determined from context
                .notificationType(Notification.NotificationType.CUSTOM)
                .channel(Notification.NotificationChannel.EMAIL)
                .title(emailRequest.getSubject())
                .content(emailRequest.getContent())
                .status(Notification.NotificationStatus.PENDING)
                .priority(emailRequest.getPriority())
                .scheduledAt(emailRequest.getScheduledAt())
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    private String sendMimeMessage(EmailRequest emailRequest, Long notificationId) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set basic email properties
        helper.setFrom(emailRequest.getSenderEmail() != null ? emailRequest.getSenderEmail() : defaultFromEmail,
                emailRequest.getSenderName() != null ? emailRequest.getSenderName() : defaultFromName);
        helper.setTo(emailRequest.getRecipientEmail());
        helper.setSubject(emailRequest.getSubject());
        helper.setText(emailRequest.getContent(), emailRequest.getIsHtml());

        // Set optional properties
        if (emailRequest.getReplyTo() != null) {
            helper.setReplyTo(emailRequest.getReplyTo());
        }

        if (emailRequest.getCcEmails() != null && !emailRequest.getCcEmails().isEmpty()) {
            helper.setCc(emailRequest.getCcEmails().toArray(new String[0]));
        }

        if (emailRequest.getBccEmails() != null && !emailRequest.getBccEmails().isEmpty()) {
            helper.setBcc(emailRequest.getBccEmails().toArray(new String[0]));
        }

        // Add custom headers
        if (emailRequest.getCustomHeaders() != null) {
            emailRequest.getCustomHeaders().forEach((key, value) -> {
                try {
                    message.setHeader(key, value);
                } catch (MessagingException e) {
                    log.warn("Failed to set custom header: {} = {}", key, value);
                }
            });
        }

        // Add tracking headers if enabled
        if (trackingEnabled && emailRequest.getEnableTracking()) {
            message.setHeader("X-Notification-ID", notificationId.toString());
            if (emailRequest.getTrackingId() != null) {
                message.setHeader("X-Tracking-ID", emailRequest.getTrackingId());
            }
        }

        // Add attachments
        if (emailRequest.getAttachments() != null) {
            for (EmailRequest.EmailAttachment attachment : emailRequest.getAttachments()) {
                if (attachment.getIsInline()) {
                    helper.addInline(attachment.getContentId(), 
                            () -> new java.io.ByteArrayInputStream(attachment.getContent()),
                            attachment.getContentType());
                } else {
                    helper.addAttachment(attachment.getFileName(), 
                            () -> new java.io.ByteArrayInputStream(attachment.getContent()),
                            attachment.getContentType());
                }
            }
        }

        // Send the message
        mailSender.send(message);

        // Return message ID (simplified - in real implementation, extract from sent message)
        return UUID.randomUUID().toString();
    }

    private void createCommunicationLog(Notification notification, String messageId, String recipientEmail) {
        CommunicationLog log = CommunicationLog.builder()
                .notificationId(notification.getId())
                .channel(Notification.NotificationChannel.EMAIL)
                .recipientEmail(recipientEmail)
                .status(CommunicationLog.LogStatus.SENT)
                .sentAt(LocalDateTime.now())
                .externalMessageId(messageId)
                .provider("JavaMail")
                .retryCount(0)
                .build();

        communicationLogRepository.save(log);
    }
}
