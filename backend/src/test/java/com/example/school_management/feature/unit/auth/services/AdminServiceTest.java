//package com.example.school_management.feature.unit.auth.services;
//
//import com.example.school_management.commons.service.EmailService;
//import com.example.school_management.feature.auth.dto.CreateStudentWithParentsRequest;
//import com.example.school_management.feature.auth.dto.ParentCreateDto;
//import com.example.school_management.feature.auth.dto.StudentCreateDto;
//import com.example.school_management.feature.auth.dto.BaseUserCreateDto;
//import com.example.school_management.feature.auth.entity.Parent;
//import com.example.school_management.feature.auth.entity.Student;
//import com.example.school_management.feature.auth.entity.UserRole;
//import com.example.school_management.feature.auth.repository.ParentRepository;
//import com.example.school_management.feature.auth.repository.StudentRepository;
//import com.example.school_management.feature.auth.service.AdminService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AdminServiceTest {
//
//    @Mock StudentRepository studentRepo;
//    @Mock ParentRepository  parentRepo;
//    @Mock PasswordEncoder   passwordEncoder;
//    @Mock EmailService      emailService;
//
//    @InjectMocks
//    AdminService adminService;
//
//    private StudentCreateDto  studentDto;
//    private ParentCreateDto  parent1, parent2;
//
//    @BeforeEach
//    void setUp() {
//        var profile = new BaseUserCreateDto(
//                               "Alice", "Anderson",
//                               "alice@example.com",
//                                "555-1234",
//                              LocalDate.of(2010,1,15),
//                                "F",
//                                "123 Maple St",
//                         UserRole.STUDENT
//                             );
//                       studentDto = new StudentCreateDto(
//                                profile,
//                               "5th Grade",
//                               2020
//                             );
//
//
//        parent1 = new ParentCreateDto(
//                "Bob", "Brown",
//                "bob@example.com",
//                "555-5678",
//                "email"
//        );
//        parent2 = new ParentCreateDto(
//                "Carol", "Clark",
//                "carol@example.com",
//                "555-9012",
//                "phone"
//        );
//
//        // make passwordEncoder return a predictable hash
//        given(passwordEncoder.encode(anyString())).willAnswer(a -> "ENC(" + a.getArgument(0) + ")");
//    }
//
//    @Test
//    void when_oneParent_then_studentAndParentSaved_and_twoEmailsSent() {
//        var req = new CreateStudentWithParentsRequest(studentDto, List.of(parent1));
//
//        adminService.createStudentWithParents(req);
//
//        // student saved
//        then(studentRepo).should().save(argThat(s -> {
//            assertThat(s).isInstanceOf(Student.class);
//            assertThat(s.getEmail()).isEqualTo("alice@example.com");
//            assertThat(s.getPassword()).startsWith("ENC(");
//            assertThat(s.getRole()).isEqualTo(UserRole.STUDENT);
//            return true;
//        }));
//
//        // parent saved
//        then(parentRepo).should().save(argThat(p -> {
//            assertThat(p).isInstanceOf(Parent.class);
//            assertThat(p.getEmail()).isEqualTo("bob@example.com");
//            assertThat(p.getPassword()).startsWith("ENC(");
//            assertThat(p.getRole()).isEqualTo(UserRole.PARENT);
//            // linked
//            assertThat(p.getChildren()).hasSize(1);
//            return true;
//        }));
//
//        // two emails: one to student, one to parent
//        then(emailService).should(times(2))
//                .sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
//    }
//
//    @Test
//    void when_twoParents_then_bothParentsSaved() {
//        var req = new CreateStudentWithParentsRequest(studentDto, List.of(parent1, parent2));
//
//        adminService.createStudentWithParents(req);
//
//        then(parentRepo).should(times(2)).save(any(Parent.class));
//        then(emailService).should(times(3))  // 1 student + 2 parents
//                .sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
//    }
//
//    @Test
//    void when_zeroParents_then_emptyList_passThrough_but_noParentsSaved() {
//        var req = new CreateStudentWithParentsRequest(studentDto, List.of());
//
//        adminService.createStudentWithParents(req);
//
//        // student still created
//        then(studentRepo).should().save(any(Student.class));
//        // no parent
//        then(parentRepo).should(never()).save(any());
//        // only one email (student)
//        then(emailService).should(times(1))
//                .sendTemplateEmail(eq("alice@example.com"), anyString(), anyString(), anyMap());
//    }
//
//    @Test
//    void when_moreThanTwoParents_then_stillProcesses_all_but_callerShouldValidateSize() {
//        var extra = new ParentCreateDto("Dave","Davis","dave@example.com","555-0000","email");
//        var req = new CreateStudentWithParentsRequest(studentDto, List.of(parent1,parent2,extra));
//
//        adminService.createStudentWithParents(req);
//
//        // service does *not* itself enforce the 2‐parent max
//        then(parentRepo).should(times(3)).save(any(Parent.class));
//        then(emailService).should(times(4)).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
//    }
//
//    @Test
//    void when_studentRepoThrowsIntegrityViolation_then_bubbleUp() {
//        willThrow(new DataIntegrityViolationException("dup email"))
//                .given(studentRepo).save(any(Student.class));
//
//        var req = new CreateStudentWithParentsRequest(studentDto, List.of(parent1));
//
//        assertThatThrownBy(() -> adminService.createStudentWithParents(req))
//                .isInstanceOf(DataIntegrityViolationException.class)
//                .hasMessageContaining("dup email");
//
//        // no parent saved or emailed
//        then(parentRepo).should(never()).save(any());
//        then(emailService).should(never()).sendTemplateEmail(any(), any(), any(), anyMap());
//    }
//
//    @Test
//    void when_parentRepoThrows_then_transactionRolledBack_studentNotPersistedTwice() {
//        // first student save OK
//        // first parent save throws
//        willThrow(new DataIntegrityViolationException("parent dup"))
//                .given(parentRepo).save(any(Parent.class));
//
//        var req = new CreateStudentWithParentsRequest(studentDto, List.of(parent1));
//
//        assertThatThrownBy(() -> adminService.createStudentWithParents(req))
//                .isInstanceOf(DataIntegrityViolationException.class);
//
//        // studentRepo.save was called once—but transaction should roll back,
//        // so depending on your DB & Spring config the eventual commit won’t happen.
//        then(studentRepo).should().save(any(Student.class));
//        // but you attempted to save parent
//        then(parentRepo).should().save(any(Parent.class));
//    }
//}
//
