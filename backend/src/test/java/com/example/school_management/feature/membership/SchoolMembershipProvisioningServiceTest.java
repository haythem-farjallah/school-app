package com.example.school_management.feature.membership;

import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.membership.service.SchoolMembershipProvisioningService;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SchoolMembershipProvisioningServiceTest {
    private final SchoolMembershipRepository memberships = mock(SchoolMembershipRepository.class);
    private final CurrentSchoolResolver schools = mock(CurrentSchoolResolver.class);
    private final SchoolMembershipProvisioningService provisioner =
            new SchoolMembershipProvisioningService(memberships, schools);

    @Test
    void staffIsANoOpEvenWithoutStatusOrPersistedId() {
        Teacher user = new Teacher();
        user.setRole(UserRole.STAFF);

        provisioner.provisionFor(user);

        verifyNoInteractions(schools, memberships);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "TEACHER", "STUDENT", "PARENT"})
    void deletedAccountsDoNotResolveSchoolOrTouchMemberships(UserRole role) {
        Teacher user = new Teacher();
        user.setRole(role);
        user.setStatus(Status.DELETED);

        provisioner.provisionFor(user);

        verifyNoInteractions(schools, memberships);
    }

    @Test
    void nullRoleFailsClearlyBeforeSchoolResolution() {
        Teacher user = new Teacher();
        user.setStatus(Status.ACTIVE);

        assertThatThrownBy(() -> provisioner.provisionFor(user))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("role");
        verifyNoInteractions(schools, memberships);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "TEACHER", "STUDENT", "PARENT"})
    void nullStatusFailsClearlyBeforeSchoolResolution(UserRole role) {
        Teacher user = new Teacher();
        user.setRole(role);

        assertThatThrownBy(() -> provisioner.provisionFor(user))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("status");
        verifyNoInteractions(schools, memberships);
    }

    @Test
    void eligibleUserMustAlreadyBePersisted() {
        Teacher user = new Teacher();
        user.setRole(UserRole.TEACHER);
        user.setStatus(Status.ACTIVE);

        assertThatThrownBy(() -> provisioner.provisionFor(user))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("persisted");
        verifyNoInteractions(schools, memberships);
    }
}
