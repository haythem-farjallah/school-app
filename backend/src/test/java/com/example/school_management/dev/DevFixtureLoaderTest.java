package com.example.school_management.dev;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.AdministrationRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class DevFixtureLoaderTest {

    @Autowired
    DevFixtureLoader loader;

    @Autowired
    AdministrationRepository administrationRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    ParentRepository parentRepository;

    @Test
    @Transactional
    void createsOneAccountPerRoleAndLinksParentToStudent() {
        assertThat(administrationRepository.findByEmail(DevFixtureLoader.ADMIN_EMAIL))
                .hasValueSatisfying(admin -> assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN));
        assertThat(teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL)).isPresent();
        assertThat(studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL)).isPresent();

        Parent parent = parentRepository.findByEmail(DevFixtureLoader.PARENT_EMAIL).orElseThrow();
        assertThat(parent.getChildren())
                .extracting(Student::getEmail)
                .containsExactly(DevFixtureLoader.STUDENT_EMAIL);
    }

    @Test
    void runningAgainDoesNotDuplicateAccounts() {
        long before = administrationRepository.count() + parentRepository.count() + studentRepository.count();
        ApplicationArguments noArgs = new DefaultApplicationArguments();

        loader.run(noArgs);

        long after = administrationRepository.count() + parentRepository.count() + studentRepository.count();
        assertThat(after).isEqualTo(before);
    }
}
