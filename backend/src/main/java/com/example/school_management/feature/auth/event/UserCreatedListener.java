package com.example.school_management.feature.auth.event;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.dto.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final EmailService mail;

    @Async          // send mail without blocking the transaction thread
    @EventListener
    public void handle(UserCreatedEvent ev) {
        var u = ev.user();
        mail.sendTemplateEmail(
                u.getEmail(),
                "Welcome to the Portal",
                "welcome-email",
                Map.of("name", u.getFirstName(),
                        "password", ev.rawPassword())
        );
        log.info("Welcome email queued for {}", u.getEmail());
    }
}
