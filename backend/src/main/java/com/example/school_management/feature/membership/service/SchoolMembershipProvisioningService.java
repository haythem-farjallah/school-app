package com.example.school_management.feature.membership.service;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolMembershipProvisioningService {
    private final SchoolMembershipRepository memberships;
    private final CurrentSchoolResolver schools;

    @Transactional
    public void provisionFor(BaseUser user) {
        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role is required for membership provisioning");
        }
        MembershipRole role = switch (user.getRole()) {
            case ADMIN -> MembershipRole.ADMIN;
            case TEACHER -> MembershipRole.TEACHER;
            case STUDENT -> MembershipRole.STUDENT;
            case PARENT -> MembershipRole.GUARDIAN;
            case STAFF -> null;
        };
        if (role == null) {
            return;
        }
        if (user.getStatus() == null) {
            throw new IllegalArgumentException("User status is required for membership provisioning");
        }
        if (user.getStatus() == Status.DELETED) {
            return;
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User must be persisted before membership provisioning");
        }

        var school = schools.resolve();
        SchoolMembership membership = memberships.findByUserIdAndSchoolId(user.getId(), school.getId())
                .orElseGet(() -> {
                    SchoolMembership created = new SchoolMembership();
                    created.setUser(user);
                    created.setSchool(school);
                    created.setStatus(switch (user.getStatus()) {
                        case ACTIVE -> MembershipStatus.ACTIVE;
                        case SUSPENDED -> MembershipStatus.SUSPENDED;
                        case DELETED -> throw new IllegalStateException("Deleted user cannot receive a membership");
                    });
                    return created;
                });
        membership.getRoles().add(role);
        memberships.save(membership);
    }
}
