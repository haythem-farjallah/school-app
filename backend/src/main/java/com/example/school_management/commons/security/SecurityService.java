package com.example.school_management.commons.security;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Identity checks for method-security expressions, e.g.
 * {@code @securityService.isCurrentUser(#studentId)}. The caller's identity is always
 * the authenticated principal's persisted account, never a value from the request.
 */
@Component("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;

    /** The id of the authenticated caller's account; empty for an anonymous request. */
    public Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName()).map(BaseUser::getId);
    }

    /** True when {@code userId} is the authenticated caller's own account. */
    public boolean isCurrentUser(Long userId) {
        return userId != null && currentUserId().map(userId::equals).orElse(false);
    }
}
