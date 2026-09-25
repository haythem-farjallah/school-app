package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.operational.entity.ResourceComment;
import com.example.school_management.feature.operational.repository.ResourceCommentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who may delete a resource comment. Tested at the service because comments cannot
 * currently be stored for a learning resource: resource_comments.on_resource_id
 * references the legacy resources table.
 */
class ResourceCommentServiceImplTest {

    private static final long COMMENT_ID = 7;

    private final ResourceCommentRepository comments = mock(ResourceCommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ResourceCommentServiceImpl service =
            new ResourceCommentServiceImpl(comments, mock(LearningResourceRepository.class), users);

    private final BaseUser author = user(new Student(), 1L, UserRole.STUDENT, "author@school.test");

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
        verify(comments, never()).deleteById(any());
    }

    @Test
    void authorCanDeleteTheirComment() {
        signIn(author);

        service.delete(COMMENT_ID);

        verify(comments).deleteById(COMMENT_ID);
    }

    @Test
    void adminCanDeleteAnyComment() {
        signIn(user(new Teacher(), 3L, UserRole.ADMIN, "admin@school.test"));

        service.delete(COMMENT_ID);

        verify(comments).deleteById(COMMENT_ID);
    }

    private void signIn(BaseUser user) {
        ResourceComment comment = new ResourceComment();
        comment.setId(COMMENT_ID);
        comment.setCommentedBy(author);
        when(comments.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
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
