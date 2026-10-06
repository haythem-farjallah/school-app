package com.example.school_management.feature.communication;

import com.example.school_management.feature.communication.dto.BulkEmailRequest;
import com.example.school_management.feature.communication.dto.EmailRequest;
import com.example.school_management.feature.communication.dto.EmailResponse;
import com.example.school_management.feature.communication.entity.NotificationTemplate;
import com.example.school_management.feature.communication.repository.CommunicationLogRepository;
import com.example.school_management.feature.communication.repository.CommunicationNotificationRepository;
import com.example.school_management.feature.communication.repository.NotificationTemplateRepository;
import com.example.school_management.feature.communication.service.NotificationTemplateService;
import com.example.school_management.feature.communication.service.impl.EmailServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailFailurePrivacyTest {
    @ParameterizedTest
    @CsvSource({"send, Email sending failed", "template, Email template processing failed",
            "schedule, Email scheduling failed", "recipient, Email recipient processing failed"})
    void unexpectedFailureKeepsExplicitFailureWithoutExposingInternalDetails(String operation, String expected) {
        var notifications = mock(CommunicationNotificationRepository.class);
        var templates = mock(NotificationTemplateRepository.class);
        var service = spy(new EmailServiceImpl(mock(JavaMailSender.class), notifications, templates,
                mock(CommunicationLogRepository.class), mock(NotificationTemplateService.class)));
        ReflectionTestUtils.setField(service, "asyncEnabled", false);
        var failure = new DataAccessResourceFailureException(
                "ERROR: relation notifications does not exist [select content from notifications] smtp.internal.test");
        doThrow(failure).when(notifications).save(any());
        when(templates.findByTemplateNameAndTemplateTypeAndLanguage("school-update", NotificationTemplate.TemplateType.EMAIL, "en"))
                .thenThrow(failure);
        EmailRequest request = EmailRequest.builder().recipientEmail("recipient@example.test")
                .subject("School update").content("Class notice").build();

        EmailResponse response = switch (operation) {
            case "send" -> service.sendEmail(request);
            case "template" -> service.sendTemplatedEmail("school-update", request.getRecipientEmail(), Map.of());
            case "schedule" -> service.scheduleEmail(request, LocalDateTime.of(2030, 1, 7, 8, 0));
            case "recipient" -> {
                doThrow(failure).when(service).sendEmail(any());
                var bulk = BulkEmailRequest.builder().subject(request.getSubject()).content(request.getContent())
                        .recipients(List.of(BulkEmailRequest.BulkEmailRecipient.builder()
                                .email(request.getRecipientEmail()).build())).build();
                yield service.sendBulkEmails(bulk).get(0);
            }
            default -> throw new AssertionError("Unknown operation");
        };

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo(expected);
        assertThat(response.getErrorMessage()).doesNotContain("select", "notifications", "smtp.internal.test");
    }
}
