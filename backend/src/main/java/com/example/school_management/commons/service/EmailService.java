package com.example.school_management.commons.service;

import com.example.school_management.feature.auth.entity.BaseUser;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender    mailSender;
    private final SpringTemplateEngine tplEngine;
    private final String             from;

    public EmailService(JavaMailSender mailSender,
                        SpringTemplateEngine tplEngine,
                        @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.tplEngine  = tplEngine;
        this.from       = from;
    }

    /**
     * Render the given Thymeleaf template + send as HTML
     * @param to           recipient
     * @param subject      email subject (also passed into template ctx)
     * @param templateName name of HTML template (without “.html”)
     * @param variables    map of variables for template
     * @throws MailException when the message cannot be built or sent. Sending is synchronous,
     *                       so the caller knows whether the email went out.
     */
    public void sendTemplateEmail(
            String to,
            String subject,
            String templateName,
            Map<String,Object> variables
    ) {
        try {
            // prepare the Thymeleaf context
            Context ctx = new Context();
            ctx.setVariables(variables);
            ctx.setVariable("subject", subject);

            // render HTML
            String html = tplEngine.process(templateName, ctx);

            // build and send
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(msg);
            log.info("📧 Email sent using template {}", templateName);
        }
        catch (MessagingException ex) {
            throw new MailPreparationException("Could not build the email", ex);
        }
    }

    /**
     * Send bulk emails to multiple recipients
     */
    @Async
    public CompletableFuture<BulkEmailResult> sendBulkEmails(
            List<? extends BaseUser> recipients,
            String subject,
            String templateName,
            Map<String, Object> baseVariables
    ) {
        log.info("📧 Starting bulk email send to {} recipients", recipients.size());
        
        List<String> successful = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        
        for (BaseUser user : recipients) {
            try {
                // Prepare personalized variables
                Map<String, Object> personalizedVariables = new java.util.HashMap<>(baseVariables);
                personalizedVariables.put("firstName", user.getFirstName());
                personalizedVariables.put("lastName", user.getLastName());
                personalizedVariables.put("email", user.getEmail());
                personalizedVariables.put("fullName", user.getFirstName() + " " + user.getLastName());
                
                // Prepare the Thymeleaf context
                Context ctx = new Context();
                ctx.setVariables(personalizedVariables);
                ctx.setVariable("subject", subject);

                // Render HTML
                String html = tplEngine.process(templateName, ctx);

                // Build and send
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(user.getEmail());
                helper.setSubject(subject);
                helper.setText(html, true);

                mailSender.send(msg);
                successful.add(user.getEmail());
                
            } catch (MailException | MessagingException ex) {
                failed.add(user.getEmail());
                log.error("❌ Failed to send bulk email to one recipient: {}", ex.getClass().getSimpleName());
            }
        }
        
        BulkEmailResult result = new BulkEmailResult(
            recipients.size(),
            successful.size(),
            failed.size(),
            successful,
            failed
        );
        
        log.info("📧 Bulk email completed: {}/{} successful", 
                result.getSuccessCount(), result.getTotalCount());
        
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Send simple text email to multiple recipients
     */
    @Async
    public CompletableFuture<BulkEmailResult> sendBulkSimpleEmails(
            List<String> recipients,
            String subject,
            String content
    ) {
        log.info("📧 Starting bulk simple email send to {} recipients", recipients.size());
        
        List<String> successful = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        
        for (String email : recipients) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(content, false); // Plain text
                
                mailSender.send(msg);
                successful.add(email);
                
            } catch (MailException | MessagingException ex) {
                failed.add(email);
                log.error("❌ Failed to send simple email to one recipient: {}", ex.getClass().getSimpleName());
            }
        }
        
        BulkEmailResult result = new BulkEmailResult(
            recipients.size(),
            successful.size(),
            failed.size(),
            successful,
            failed
        );
        
        log.info("📧 Bulk simple email completed: {}/{} successful", 
                result.getSuccessCount(), result.getTotalCount());
        
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Result class for bulk email operations
     */
    public static class BulkEmailResult {
        private final int totalCount;
        private final int successCount;
        private final int failureCount;
        private final List<String> successful;
        private final List<String> failed;

        public BulkEmailResult(int totalCount, int successCount, int failureCount, 
                              List<String> successful, List<String> failed) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.successful = successful;
            this.failed = failed;
        }

        // Getters
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<String> getSuccessful() { return successful; }
        public List<String> getFailed() { return failed; }
        public double getSuccessRate() { 
            return totalCount > 0 ? (double) successCount / totalCount * 100 : 0; 
        }
    }
}
