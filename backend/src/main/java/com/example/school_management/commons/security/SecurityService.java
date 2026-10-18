package com.example.school_management.commons.security;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Identity and school relationship checks for services and method-security expressions, e.g.
 * {@code @securityService.isCurrentUser(#studentId)}. The caller's identity is always
 * the authenticated principal's persisted account, never a value from the request.
 */
@Component("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final SchoolMembershipRepository memberships;

    /** The id of the authenticated caller's account; empty for an anonymous request. */
    public Optional<Long> currentUserId() {
        return currentUser().map(BaseUser::getId);
    }

    private Optional<BaseUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    /** True when {@code userId} is the authenticated caller's own account. */
    public boolean isCurrentUser(Long userId) {
        return userId != null && currentUserId().map(userId::equals).orElse(false);
    }

    /** Administrative read access requires both the account authority and an ADMIN relationship to this school. */
    @Transactional(readOnly = true)
    public boolean isSchoolAdministrator(Long schoolId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (schoolId == null || authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ADMIN_READ_WRITE".equals(authority.getAuthority()))) {
            return false;
        }
        return currentUser()
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .flatMap(user -> memberships.findByUserIdAndSchoolId(user.getId(), schoolId))
                .map(membership -> membership.getRoles().contains(MembershipRole.ADMIN))
                .orElse(false);
    }
}
