package com.example.school_management.commons.service;

import java.util.Map;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
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
     */
    @Async
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
            log.info("📧 Email sent to {}", to);
        }
        catch (MailException | MessagingException ex) {
            log.error("❌ Failed to send email to {}: {}", to, ex.getMessage(), ex);
        }
    }
}
