package com.example.school_management.dev;

import com.example.school_management.feature.auth.entity.Administration;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.entity.enums.Relation;
import com.example.school_management.feature.auth.repository.AdministrationRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic accounts for local development and browser tests.
 * Loaded only with the "fixtures" profile (included by "dev"); never in production.
 */
@Component
@Profile("fixtures")
@RequiredArgsConstructor
@Slf4j
public class DevFixtureLoader implements ApplicationRunner {

    public static final String PASSWORD = "Fixture-Pass-2024";
    public static final String ADMIN_EMAIL = "admin@fixtures.school.test";
    public static final String TEACHER_EMAIL = "teacher@fixtures.school.test";
    public static final String STUDENT_EMAIL = "student@fixtures.school.test";
    public static final String PARENT_EMAIL = "parent@fixtures.school.test";

    private final UserRepository userRepository;
    private final AdministrationRepository administrationRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Fixture accounts already present");
            return;
        }

        administrationRepository.save(account(new Administration(), UserRole.ADMIN, ADMIN_EMAIL, "Ada", "Admin"));
        teacherRepository.save(account(new Teacher(), UserRole.TEACHER, TEACHER_EMAIL, "Theo", "Teacher"));
        Student student = studentRepository.save(account(new Student(), UserRole.STUDENT, STUDENT_EMAIL, "Sam", "Student"));

        Parent parent = account(new Parent(), UserRole.PARENT, PARENT_EMAIL, "Pat", "Parent");
        parent.setRelation(Relation.GUARDIAN);
        parent.getChildren().add(student);
        parentRepository.save(parent);

        log.info("Fixture accounts created: admin, teacher, student, parent");
    }

    private <U extends BaseUser> U account(U user, UserRole role, String email, String firstName, String lastName) {
        user.setRole(role);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setStatus(Status.ACTIVE);
        user.setPasswordChangeRequired(false);
        user.setIsEmailVerified(true);
        return user;
    }
}
