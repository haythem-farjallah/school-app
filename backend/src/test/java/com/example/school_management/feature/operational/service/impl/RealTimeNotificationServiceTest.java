package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.RealTimeNotificationDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Where realtime notifications are delivered: a notification about one student reaches only that
 * student's (and a given parent's) personal queue, never a topic shared by a whole role.
 */
class RealTimeNotificationServiceTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final RealTimeNotificationService service = new RealTimeNotificationService(messagingTemplate);

    @Test
    void enrollmentWithoutParentGoesToTheStudentsQueueOnly() {
        service.notifyEnrollmentChange("Sam Student", "7A", "ENROLLED", 10L, null);

        assertThat(destinations(1)).containsExactly("/queue/user/10/notifications");
    }

    @Test
    void enrollmentWithParentGoesToTheStudentAndParentQueuesOnly() {
        service.notifyEnrollmentChange("Sam Student", "7A", "ENROLLED", 10L, 20L);

        assertThat(destinations(2))
                .containsExactly("/queue/user/10/notifications", "/queue/user/20/notifications");
    }

    @Test
    void announcementGoesToTheTargetedRoleTopicsOnly() {
        service.notifyNewAnnouncement("Sports day", "Friday", "NORMAL", Set.of("TEACHER"));

        assertThat(destinations(1)).containsExactly("/topic/notifications/teacher");
    }

    /** The destinations of exactly {@code expected} sends, in order. */
    private List<String> destinations(int expected) {
        ArgumentCaptor<String> destinations = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(expected))
                .convertAndSend(destinations.capture(), any(RealTimeNotificationDto.class));
        return destinations.getAllValues();
    }
}
