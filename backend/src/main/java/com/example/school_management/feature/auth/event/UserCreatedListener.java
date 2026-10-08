package com.example.school_management.feature.auth.event;

import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.auth.dto.UserCreatedEvent;
import com.example.school_management.feature.auth.dto.WelcomeEmailRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserCreatedEvent ev) {
        var u = ev.user();
        sendWelcomeEmail(new WelcomeEmailRequestedEvent(u.getId(), u.getEmail(), u.getFirstName(), ev.rawPassword()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWelcomeEmailRequest(WelcomeEmailRequestedEvent ev) {
        sendWelcomeEmail(ev);
    }

    private void sendWelcomeEmail(WelcomeEmailRequestedEvent ev) {
        emailService.sendWelcomeEmail(
                ev.recipientId(),
                ev.recipientEmail(),
                ev.userName(),
                ev.rawPassword()
        );

        log.info("Welcome email processed for user id={}", ev.recipientId());
    }
}
