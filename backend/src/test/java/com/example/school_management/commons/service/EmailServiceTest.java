package com.example.school_management.commons.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void templateEmailReportsSendFailureToTheCaller() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        when(templateEngine.process(eq("otp"), any(IContext.class))).thenReturn("<p>code</p>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));
        EmailService emailService = new EmailService(mailSender, templateEngine, "no-reply@school.test");

        assertThatThrownBy(() -> emailService.sendTemplateEmail(
                "user@accounts.school.test", "Subject", "otp", Map.of("code", "000000")))
                .isInstanceOf(MailSendException.class);
    }
}
