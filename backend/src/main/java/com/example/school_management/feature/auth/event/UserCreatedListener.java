package com.example.school_management.feature.auth.event;

import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.auth.dto.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final EmailService emailService;

    @Async          // send mail without blocking the transaction thread
    @EventListener
    public void handle(UserCreatedEvent ev) {
        var u = ev.user();
        
        // Use the communication email service with proper welcome email method
        emailService.sendWelcomeEmail(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                ev.rawPassword()
        );
        
        log.info("Welcome email queued for {}", u.getEmail());
    }
}
