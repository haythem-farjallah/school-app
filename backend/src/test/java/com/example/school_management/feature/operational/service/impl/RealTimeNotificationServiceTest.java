package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.RealTimeNotificationDto;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
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
import static org.mockito.Mockito.verifyNoInteractions;

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
    void specificUsersReceiveOnlyTheirOwnRecipientId() {
        service.notifySpecificUsers("Sports day", "Friday", "MEDIUM", Set.of(10L, 20L));

        ArgumentCaptor<String> destinations = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RealTimeNotificationDto> payloads = ArgumentCaptor.forClass(RealTimeNotificationDto.class);
        verify(messagingTemplate, times(2)).convertAndSend(destinations.capture(), payloads.capture());
        assertThat(destinations.getAllValues()).containsExactlyInAnyOrder("/queue/user/10/notifications", "/queue/user/20/notifications");
        for (int i = 0; i < 2; i++) {
            Set<Long> targetIds = payloads.getAllValues().get(i).getTargetUserIds();
            assertThat(targetIds).hasSize(1);
            assertThat(destinations.getAllValues().get(i)).isEqualTo("/queue/user/" + targetIds.iterator().next() + "/notifications");
        }
    }

    @Test
    void announcementAuditCannotBroadcastAnnouncementContentGlobally() {
        service.broadcastAdminFeed(AuditEventType.ANNOUNCEMENT_CREATED, "Created", "Private content", "Teacher", "Announcement", 1L);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void unrelatedAdminAuditDestinationIsPreserved() {
        service.broadcastAdminFeed(AuditEventType.GRADE_RECORDED, "Recorded", "Details", "Teacher", "Grade", 1L);
        assertThat(destinations(1)).containsExactly("/topic/admin-feeds");
    }

    /** The destinations of exactly {@code expected} sends, in order. */
    private List<String> destinations(int expected) {
        ArgumentCaptor<String> destinations = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(expected))
                .convertAndSend(destinations.capture(), any(RealTimeNotificationDto.class));
        return destinations.getAllValues();
    }
}
