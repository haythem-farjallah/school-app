package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.auth.dto.*;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.service.AdminService;
import com.example.school_management.feature.auth.service.TeacherService;
import com.example.school_management.feature.communication.dto.EmailRequest;
import com.example.school_management.feature.communication.service.impl.EmailServiceImpl;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

// No wrapping test transaction: assertions distinguish committed state from rollback.
@IntegrationTest
@Import(WelcomeEmailTransactionIntegrationTest.AsyncConfiguration.class)
class WelcomeEmailTransactionIntegrationTest {
    @Autowired TeacherService teachers;
    @Autowired AdminService families;
    @Autowired EmailServiceImpl emails;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ControlledExecutor executor;
    @MockitoBean JavaMailSender mailSender;

    private final List<MimeMessage> delivered = new ArrayList<>();
    private final List<Long> accountIds = new ArrayList<>();
    private String teacherEmail;
    private String studentEmail;
    private String parentEmail;

    @BeforeEach
    void prepareMailTransport() {
        teacherEmail = UUID.randomUUID() + "@welcome.test";
        studentEmail = UUID.randomUUID() + "@welcome.test";
        parentEmail = UUID.randomUUID() + "@welcome.test";
        when(mailSender.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(call -> {
            MimeMessage message = call.getArgument(0);
            message.saveChanges();
            delivered.add(message);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));
    }

    @AfterEach
    void cleanupOnlyPacketAccounts() throws Exception {
        executor.drain();
        accountIds.addAll(jdbc.queryForList("SELECT id FROM users WHERE email IN (?, ?, ?)",
                Long.class, teacherEmail, studentEmail, parentEmail));
        for (Long id : accountIds) {
            jdbc.update("DELETE FROM communication_logs WHERE notification_id IN (SELECT id FROM notifications WHERE recipient_id = ?)", id);
            jdbc.update("DELETE FROM notifications WHERE recipient_id = ?", id);
            jdbc.update("DELETE FROM profile_settings WHERE user_id = ?", id);
            jdbc.update("DELETE FROM audit_events WHERE entity_id = ? AND entity_type IN ('TEACHER', 'STUDENT', 'PARENT')", id);
            jdbc.update("DELETE FROM parent_students WHERE parent_id = ? OR student_id = ?", id, id);
            jdbc.update("DELETE FROM school_membership_roles WHERE membership_id IN (SELECT id FROM school_memberships WHERE user_id = ?)", id);
            jdbc.update("DELETE FROM school_memberships WHERE user_id = ?", id);
            jdbc.update("DELETE FROM teacher WHERE id = ?", id);
            jdbc.update("DELETE FROM student WHERE id = ?", id);
            jdbc.update("DELETE FROM parent WHERE id = ?", id);
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "family"})
    void committedCreationSendsOneWelcomePerAccountAndPersistsSafeContent(String path) throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> create(path));
        executor.drain();

        assertThat(delivered).hasSize(accountIds.size());
        for (Long id : accountIds) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ? AND password_change_required = true", Long.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships WHERE user_id = ?", Long.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND status = 'SENT'", Long.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM communication_logs l JOIN notifications n ON n.id = l.notification_id WHERE n.recipient_id = ? AND l.status = 'SENT'", Long.class, id)).isEqualTo(1);

            String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, id);
            MimeMessage message = delivered.stream().filter(value -> recipient(value).equals(email)).findFirst().orElseThrow();
            String body = html(message.getContent());
            var credential = Pattern.compile("<strong>(.*?)</strong>").matcher(body);
            assertThat(credential.find()).as("welcome email contains its temporary credential").isTrue();
            String rawPassword = credential.group(1);
            String encodedPassword = jdbc.queryForObject("SELECT password FROM users WHERE id = ?", String.class, id);
            assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).as("emailed credential matches the account password").isTrue();

            String content = jdbc.queryForObject("SELECT content FROM notifications WHERE recipient_id = ?", String.class, id);
            assertThat(content.contains(rawPassword)).as("notification excludes the temporary credential").isFalse();
            assertThat(content.contains(encodedPassword)).as("notification excludes the encoded credential").isFalse();
            assertThat(content.contains("Your account has been created successfully")).as("safe account confirmation remains").isTrue();
            assertThat(content.contains("https://schoolmanagement.com/login")).as("existing login link remains").isTrue();
            String title = jdbc.queryForObject("SELECT title FROM notifications WHERE recipient_id = ?", String.class, id);
            assertThat(title.contains(rawPassword)).as("notification title excludes the temporary credential").isFalse();
        }
        if (path.equals("family")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM parent_students WHERE parent_id = ? AND student_id = ?",
                    Long.class, accountIds.get(1), accountIds.get(0))).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "family"})
    void noWelcomeEmailOrNotificationBeforeCreationCommits(String path) throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            create(path);
            drainExecutor();
            assertThat(delivered).isEmpty();
            for (Long id : accountIds) {
                assertThat(notificationCount(id)).isZero();
            }
        });
        executor.drain();
        assertThat(delivered).hasSize(accountIds.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "family"})
    void rolledBackCreationSendsNothingAndCommitsNoAccountOrNotification(String path) throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            create(path);
            drainExecutor();
            tx.setRollbackOnly();
        });
        executor.drain();

        for (Long id : accountIds) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Long.class, id)).isZero();
            assertThat(notificationCount(id)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships WHERE user_id = ?", Long.class, id)).isZero();
        }
        assertThat(delivered).isEmpty();
    }

    @Test
    void customizedWelcomeSubjectCannotPersistCredentials() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            create("teacher");
            jdbc.update("UPDATE notification_templates SET subject = 'Welcome {{temporaryPassword}}' WHERE template_name = 'welcome-email'");
            String rawPassword = UUID.randomUUID().toString();
            var response = emails.sendWelcomeEmail(accountIds.get(0), teacherEmail, "New", rawPassword);
            assertThat(response.getSuccess()).isTrue();
            try {
                assertThat(delivered.get(0).getSubject().contains(rawPassword)).as("email subject still uses the configured template").isTrue();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to read test email subject", e);
            }
            String stored = jdbc.queryForObject("SELECT row_to_json(n)::text FROM notifications n WHERE id = ?", String.class, response.getNotificationId());
            assertThat(stored.contains(rawPassword)).as("persisted welcome notification excludes credentials in every field").isFalse();
            tx.setRollbackOnly();
        });
    }

    @Test
    void ordinaryEmailRetainsItsPersistedContent() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            create("teacher");
            Long id = accountIds.get(0);
            var response = emails.sendEmailWithRecipientId(EmailRequest.builder()
                    .recipientEmail(teacherEmail).subject("School update").content("The library opens on Monday.").build(), id);
            assertThat(response.getSuccess()).isTrue();
            assertThat(jdbc.queryForObject("SELECT content FROM notifications WHERE id = ?", String.class, response.getNotificationId()))
                    .isEqualTo("The library opens on Monday.");
            tx.setRollbackOnly();
        });
    }

    private void create(String path) {
        if (path.equals("teacher")) {
            var teacher = teachers.create(new TeacherCreateDto(profile(teacherEmail, UserRole.TEACHER), "M.Ed", "Mathematics", 20, null));
            accountIds.add(teacher.getId());
        } else {
            families.createStudentWithParents(new CreateStudentWithParentsRequest(
                    new StudentDtoCreate("New", "Student", studentEmail, "12345678", LocalDateTime.of(2010, 1, 1, 0, 0), "F", "Address", "HIGH", 2026),
                    List.of(new ParentCreateDto(profile(parentEmail, UserRole.PARENT), "EMAIL", "GUARDIAN", List.of()))));
            accountIds.add(jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, studentEmail));
            accountIds.add(jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, parentEmail));
        }
    }

    private BaseUserCreateDto profile(String email, UserRole role) {
        return new BaseUserCreateDto("New", "Account", email, null, null, null, null, role);
    }

    private Long notificationCount(Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ?", Long.class, id);
    }

    private void drainExecutor() {
        try {
            executor.drain();
        } catch (Exception e) {
            throw new IllegalStateException("Async delivery did not complete", e);
        }
    }

    private static String recipient(MimeMessage message) {
        try {
            return message.getAllRecipients()[0].toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read test email recipient", e);
        }
    }

    private static String html(Object content) throws Exception {
        if (content instanceof String text) return text;
        Multipart parts = (Multipart) content;
        for (int i = 0; i < parts.getCount(); i++) {
            BodyPart part = parts.getBodyPart(i);
            if (part.isMimeType("text/html")) return (String) part.getContent();
            if (part.getContent() instanceof Multipart nested) return html(nested);
        }
        throw new IllegalStateException("No HTML in test email");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AsyncConfiguration {
        @Bean(name = "taskExecutor")
        ControlledExecutor taskExecutor() {
            return new ControlledExecutor();
        }
    }

    static class ControlledExecutor implements TaskExecutor {
        private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void drain() throws Exception {
            // A distinct thread preserves production @Async transaction isolation.
            var worker = Executors.newSingleThreadExecutor();
            try {
                Runnable task;
                while ((task = tasks.poll()) != null) {
                    worker.submit(task).get(30, TimeUnit.SECONDS);
                }
            } finally {
                worker.shutdownNow();
            }
        }
    }
}
