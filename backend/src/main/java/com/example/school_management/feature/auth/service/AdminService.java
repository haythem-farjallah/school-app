package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.dto.CreateStudentWithParentsRequest;
import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDtoCreate;
import com.example.school_management.feature.auth.dto.UserProfileDTO;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final StudentRepository studentRepo;
    private final ParentRepository  parentRepo;
    private final PasswordEncoder   passwordEncoder;
    private final EmailService      emailService;
    private final SecureRandom      secureRandom = new SecureRandom();

    @Transactional
    public void createStudentWithParents(CreateStudentWithParentsRequest req) {
        // 1) Create & save the student
        StudentDtoCreate sd = req.student();
        UserProfileDTO studentProfile = new UserProfileDTO(
                sd.firstName(),
                sd.lastName(),
                sd.email(),
                sd.telephone(),
                sd.birthday(),
                sd.gender(),
                sd.address(),
                UserRole.STUDENT
            );    
        Student student = new Student();
        populateCommon(student, studentProfile);
        String studentPw = generateAndSetPassword(student);
        student.setGradeLevel(sd.gradeLevel());
        student.setEnrollmentYear(sd.enrollmentYear());
        studentRepo.save(student);
        log.info("Created STUDENT {} (id={})", student.getEmail(), student.getId());
        emailService.sendTemplateEmail(
                student.getEmail(),
                "Welcome to Our School",
                "welcome-email",
                Map.of("name", student.getFirstName(), "password", studentPw)
        );

        // 2) Create each parent, link, save & notify
        for (ParentCreateDto pd : req.parents()) {
                UserProfileDTO parentProfile = new UserProfileDTO(
                        pd.firstName(),
                        pd.lastName(),
                        pd.email(),
                        pd.telephone(),
                        null,      // no birthday for parent
                        null,      // no gender for parent
                        null,      // no address for parent
                        UserRole.PARENT
                    );
            Parent parent = new Parent();
            populateCommon(parent, parentProfile);
            parent.setPreferredContactMethod(pd.preferredContactMethod());

            String parentPw = generateAndSetPassword(parent);
            parent.getChildren().add(student);

            parentRepo.save(parent);
            log.info("Created PARENT {} (id={}) and linked to STUDENT {}",
                    parent.getEmail(), parent.getId(), student.getEmail());
            emailService.sendTemplateEmail(
                    parent.getEmail(),
                    "Your Parent Portal Account",
                    "welcome-email",
                    Map.of("name", parent.getFirstName(), "password", parentPw)
            );
        }
    }

    /** Helper to populate all the BaseUser fields except password. */
    private void populateCommon(BaseUser u, UserProfileDTO profile) {
        u.setFirstName(profile.firstName());
        u.setLastName(profile.lastName());
        u.setEmail(profile.email());
        u.setTelephone(profile.telephone());
        u.setBirthday(profile.birthday());
        u.setGender(profile.gender());
        u.setAddress(profile.address());
        u.setRole(profile.role());
    }

    /**
     * Generates a random 8-char password, encodes it & marks the user as requiring
     * a first-login change. Returns the raw password for inclusion in email.
     */
    private String generateAndSetPassword(com.example.school_management.feature.auth.entity.BaseUser u) {
        String raw = RandomStringUtils.random(8, 0, 0, true, true, null, secureRandom);
        u.setPassword(passwordEncoder.encode(raw));
        u.setPasswordChangeRequired(true);
        return raw;
    }
}
