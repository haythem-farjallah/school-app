package com.example.school_management.feature.communication;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.communication.dto.PushNotificationRequest;
import com.example.school_management.feature.communication.dto.SMSRequest;
import com.example.school_management.feature.communication.repository.CommunicationLogRepository;
import com.example.school_management.feature.communication.repository.CommunicationNotificationRepository;
import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.communication.service.SMSService;
import com.example.school_management.feature.communication.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class CommunicationHonestyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SMSService sms;
    @Autowired PushNotificationService push;
    @Autowired EmailService email;
    @Autowired CommunicationNotificationRepository notifications;
    @Autowired CommunicationLogRepository logs;

    @Test
    void unavailableProvidersFailBeforeNotificationOrLogWrites() throws Exception {
        long beforeNotifications = notifications.count();
        long beforeLogs = logs.count();
        for (var route : List.of(
                post("/api/notifications/sms/send").content("{\"recipientPhone\":\"+12125550100\",\"message\":\"Hello\"}"),
                post("/api/notifications/sms/send-templated").param("templateName", "welcome")
                        .param("recipientPhone", "+12125550100").content("{}"),
                post("/api/notifications/sms/send-bulk").content("{\"recipients\":[{\"phone\":\"+12125550100\"}],\"message\":\"Hello\"}"),
                post("/api/notifications/push/send").content("{\"recipientId\":\"42\",\"title\":\"Hello\",\"body\":\"Hello\"}"),
                post("/api/notifications/push/send-bulk").content("{\"recipients\":[{\"recipientId\":\"42\"}],\"title\":\"Hello\",\"body\":\"Hello\"}"),
                post("/api/notifications/push/broadcast").content("{\"recipientId\":\"42\",\"title\":\"Hello\",\"body\":\"Hello\"}"))) {
            mvc.perform(route.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").exists()).andExpect(jsonPath("$.title").value("Service Unavailable"))
                    .andExpect(jsonPath("$.status").value(503)).andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.instance").exists());
        }
        assertThat(notifications.count()).isEqualTo(beforeNotifications);
        assertThat(logs.count()).isEqualTo(beforeLogs);
    }

    @Test
    void providerLookupsAndLegacyAudienceHelpersAreExplicitlyUnavailable() {
        unavailable(() -> sms.getSMSStatus("unknown"), "SMS_PROVIDER_NOT_CONFIGURED");
        unavailable(sms::getAccountBalance, "SMS_PROVIDER_NOT_CONFIGURED");
        unavailable(() -> push.getNotificationStatus("unknown"), "FCM_PROVIDER_NOT_CONFIGURED");
        unavailable(() -> email.getEmailStatus("unknown"), "EMAIL_DELIVERY_STATUS_UNAVAILABLE");
        var request = PushNotificationRequest.builder().recipientId("42").title("Hello").body("Hello").build();
        unavailable(() -> push.sendToRole("STUDENT", request), "PUSH_AUDIENCE_UNAVAILABLE");
        unavailable(() -> push.sendToClass(1L, request), "PUSH_AUDIENCE_UNAVAILABLE");
        unavailable(() -> push.sendToAllUsers(request), "PUSH_AUDIENCE_UNAVAILABLE");
        unavailable(() -> push.sendTemplatedPushNotification("welcome", "42", Map.of()), "FCM_PROVIDER_NOT_CONFIGURED");
        unavailable(() -> push.schedulePushNotification(request, LocalDateTime.now().plusDays(1)), "FCM_PROVIDER_NOT_CONFIGURED");
        unavailable(() -> push.retryFailedNotification(1L), "FCM_PROVIDER_NOT_CONFIGURED");
        unavailable(() -> sms.retryFailedSMS(1L), "SMS_PROVIDER_NOT_CONFIGURED");
        var smsRequest = SMSRequest.builder().recipientPhone("+12125550100").message("Hello").build();
        unavailable(() -> sms.scheduleSMS(smsRequest, LocalDateTime.now().plusDays(1)), "SMS_PROVIDER_NOT_CONFIGURED");
        unavailable(() -> sms.sendBulkTemplatedSMS("welcome", List.of(), Map.of()), "SMS_PROVIDER_NOT_CONFIGURED");
        unavailable(() -> push.sendEmergencyAlert("Alert", "Hello", Map.of()), "PUSH_AUDIENCE_UNAVAILABLE");
    }

    @Test
    void healthReportsCapabilitiesAndConnectionsDoNotInventPresence() throws Exception {
        String body = mvc.perform(get("/api/notifications/health").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailService").value("configured_unverified"))
                .andExpect(jsonPath("$.data.smsService").value("notConfigured"))
                .andExpect(jsonPath("$.data.pushNotificationService").value("notConfigured"))
                .andExpect(jsonPath("$.data.webSocketService").value("enabled"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("healthy");
        mvc.perform(get("/api/notifications/realtime/connections").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.trackingAvailable").value(false))
                .andExpect(jsonPath("$.data.activeConnections").doesNotExist())
                .andExpect(jsonPath("$.data.connectedUsers").doesNotExist());
        unavailable(push::getActiveConnectionsCount, "CONNECTION_TRACKING_UNAVAILABLE");
        unavailable(push::getConnectedUsers, "CONNECTION_TRACKING_UNAVAILABLE");
    }

    private void unavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, String reason) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(ex.getReason()).isEqualTo(reason);
        });
    }
}
