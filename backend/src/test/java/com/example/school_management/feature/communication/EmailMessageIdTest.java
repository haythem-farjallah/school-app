package com.example.school_management.feature.communication;

import com.example.school_management.feature.communication.dto.EmailRequest;
import com.example.school_management.feature.communication.entity.CommunicationLog;
import com.example.school_management.feature.communication.entity.Notification;
import com.example.school_management.feature.communication.repository.CommunicationLogRepository;
import com.example.school_management.feature.communication.repository.CommunicationNotificationRepository;
import com.example.school_management.feature.communication.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailMessageIdTest {
    @Test
    void responseAndLogUseTheActualSentMimeMessageId() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        var notifications = mock(CommunicationNotificationRepository.class);
        var logs = mock(CommunicationLogRepository.class);
        var service = new EmailServiceImpl(sender, notifications, null, logs, null);
        ReflectionTestUtils.setField(service, "defaultFromEmail", "school@example.test");
        ReflectionTestUtils.setField(service, "defaultFromName", "School");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        // JavaMail assigns Message-ID when saving the MIME message before SMTP transport.
        doAnswer(call -> { message.saveChanges(); return null; }).when(sender).send(message);
        when(notifications.save(any())).thenAnswer(call -> {
            Notification notification = call.getArgument(0);
            notification.setId(42L);
            return notification;
        });
        var response = service.sendEmailWithRecipientId(EmailRequest.builder()
                .recipientEmail("recipient@example.test").subject("School update").content("Hello").build(), 7L);
        assertThat(response.getSuccess()).isTrue();
        assertThat(message.getMessageID()).isNotBlank();
        assertThat(response.getMessageId()).isEqualTo(message.getMessageID());
        var log = ArgumentCaptor.forClass(CommunicationLog.class);
        verify(logs).save(log.capture());
        assertThat(log.getValue().getExternalMessageId()).isEqualTo(message.getMessageID());
        assertThat(log.getValue().getStatus()).isEqualTo(CommunicationLog.LogStatus.SENT);
        verify(sender).send(message);
    }

    @Test
    void actualMailFailureCannotCreateSentLog() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        var notifications = mock(CommunicationNotificationRepository.class);
        var logs = mock(CommunicationLogRepository.class);
        var service = new EmailServiceImpl(sender, notifications, null, logs, null);
        ReflectionTestUtils.setField(service, "defaultFromEmail", "school@example.test");
        ReflectionTestUtils.setField(service, "defaultFromName", "School");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        when(notifications.save(any())).thenAnswer(call -> call.getArgument(0));
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(message);
        var response = service.sendEmailWithRecipientId(EmailRequest.builder()
                .recipientEmail("recipient@example.test").subject("School update").content("Hello").build(), 7L);
        assertThat(response.getSuccess()).isFalse();
        verifyNoInteractions(logs);
        verify(sender).send(message);
    }
}
