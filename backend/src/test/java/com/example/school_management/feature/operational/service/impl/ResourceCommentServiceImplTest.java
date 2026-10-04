package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.academic.service.LearningResourceService;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.operational.entity.ResourceComment;
import com.example.school_management.feature.operational.repository.ResourceCommentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused authorization coverage for deleting resource comments after the School boundary check.
 */
class ResourceCommentServiceImplTest {

    private static final long COMMENT_ID = 7;
    private static final long SCHOOL_ID = 9;

    private final ResourceCommentRepository comments = mock(ResourceCommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentSchoolResolver currentSchool = mock(CurrentSchoolResolver.class);
    private final SchoolMembershipRepository memberships = mock(SchoolMembershipRepository.class);
    private final ResourceCommentServiceImpl service =
            new ResourceCommentServiceImpl(comments, mock(LearningResourceRepository.class),
                    mock(LearningResourceService.class), users, currentSchool, memberships);

    private final BaseUser author = user(new Student(), 1L, UserRole.STUDENT, "author@school.test");

    @BeforeEach
    void resolveCurrentSchool() {
        School school = new School();
        ReflectionTestUtils.setField(school, "id", SCHOOL_ID);
        when(currentSchool.resolve()).thenReturn(school);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anotherUserCannotDeleteTheComment() {
        signIn(user(new Teacher(), 2L, UserRole.TEACHER, "teacher@school.test"));

        assertThatThrownBy(() -> service.delete(COMMENT_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only delete your own comments");
        verify(comments, never()).delete(any(ResourceComment.class));
    }

    @Test
    void authorCanDeleteTheirComment() {
        signIn(author);

        service.delete(COMMENT_ID);

        verify(comments).delete(any(ResourceComment.class));
    }

    @Test
    void adminCanDeleteAnyComment() {
        BaseUser admin = user(new Teacher(), 3L, UserRole.ADMIN, "admin@school.test");
        signIn(admin);
        SchoolMembership membership = new SchoolMembership();
        membership.setRoles(Set.of(MembershipRole.ADMIN));
        when(memberships.findByUserIdAndSchoolId(admin.getId(), SCHOOL_ID)).thenReturn(Optional.of(membership));

        service.delete(COMMENT_ID);

        verify(comments).delete(any(ResourceComment.class));
    }

    @Test
    void accountRoleAloneDoesNotAllowAdminDeletion() {
        signIn(user(new Teacher(), 4L, UserRole.ADMIN, "outsider@school.test"));

        assertThatThrownBy(() -> service.delete(COMMENT_ID))
                .isInstanceOf(AccessDeniedException.class);
        verify(comments, never()).delete(any(ResourceComment.class));
    }

    @Test
    void foreignCommentIsNotFoundBeforeAuthorAuthorization() {
        signIn(author);
        when(comments.findByIdAndOnResourceSchoolId(COMMENT_ID, SCHOOL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(COMMENT_ID)).isInstanceOf(ResourceNotFoundException.class);
        verify(comments, never()).delete(any(ResourceComment.class));
    }

    private void signIn(BaseUser user) {
        ResourceComment comment = new ResourceComment();
        comment.setId(COMMENT_ID);
        comment.setCommentedBy(author);
        when(comments.findByIdAndOnResourceSchoolId(COMMENT_ID, SCHOOL_ID)).thenReturn(Optional.of(comment));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
    }

    private static BaseUser user(BaseUser user, long id, UserRole role, String email) {
        user.setId(id);
        user.setRole(role);
        user.setEmail(email);
        return user;
    }
}
