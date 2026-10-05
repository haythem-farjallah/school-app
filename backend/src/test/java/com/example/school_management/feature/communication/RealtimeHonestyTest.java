package com.example.school_management.feature.communication;

import com.example.school_management.commons.exceptions.GlobalExceptionHandler;
import com.example.school_management.feature.communication.controller.CommunicationController;
import com.example.school_management.feature.communication.service.impl.PushNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RealtimeHonestyTest {
    private final SimpMessagingTemplate broker = mock(SimpMessagingTemplate.class);
    private final PushNotificationServiceImpl push = new PushNotificationServiceImpl(null, null, broker);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new CommunicationController(null, null, push))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void absentConfigurationAndDisabledWebsocketAreReportedHonestly() throws Exception {
        mvc.perform(get("/api/notifications/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.emailService").value("notConfigured"))
                .andExpect(jsonPath("$.data.webSocketService").value("disabled"));
    }

    @Test
    void disabledWebsocketFailsInsteadOfClaimingSuccess() throws Exception {
        for (var route : List.of(post("/api/notifications/realtime/send").param("userId", "42"),
                post("/api/notifications/realtime/broadcast"))) {
            mvc.perform(route.contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.detail").value("WEBSOCKET_DISABLED"));
        }
        assertThatThrownBy(() -> push.sendRealTimeNotificationToUsers(List.of(), Map.of()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(broker);
    }

    @Test
    void brokerFailurePropagatesAsAnExplicitFailure() throws Exception {
        ReflectionTestUtils.setField(push, "webSocketEnabled", true);
        doThrow(new MessagingException("internal broker details")).when(broker).convertAndSend(anyString(), anyMap());
        for (var route : List.of(post("/api/notifications/realtime/send").param("userId", "42"),
                post("/api/notifications/realtime/broadcast"))) {
            mvc.perform(route.contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.detail").value("WEBSOCKET_DISPATCH_FAILED"));
        }
    }

    @Test
    void successfulDispatchDoesNotClaimVerifiedDelivery() throws Exception {
        ReflectionTestUtils.setField(push, "webSocketEnabled", true);
        mvc.perform(post("/api/notifications/realtime/send").param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("Real-time notification dispatched"));
        verify(broker).convertAndSend("/user/42/notifications", Map.of("message", "Hello"));
        mvc.perform(post("/api/notifications/realtime/broadcast").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("Real-time notification broadcast dispatched"));
        verify(broker).convertAndSend("/topic/notifications", Map.of());
    }
}
