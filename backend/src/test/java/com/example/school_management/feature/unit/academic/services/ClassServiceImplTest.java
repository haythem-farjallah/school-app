package com.example.school_management.feature.unit.academic.services;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.*;
import com.example.school_management.feature.academic.service.impl.ClassServiceImpl;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClassServiceImplTest {

    @Mock ClassRepository classRepo;
    @Mock CourseRepository courseRepo;
    @Mock StudentRepository studentRepo;
    @Mock TeacherRepository teacherRepo;
    @Mock AcademicMapper mapper;
    @Mock TeachingAssignmentRepository assignmentRepo;
    @Mock AuditService auditService;
    @Mock BaseUserRepository<BaseUser> userRepo;

    @InjectMocks ClassServiceImpl classService;

    private ClassEntity classEntity;
    private ClassDto classDto;

    @BeforeEach
    void setUp() {
        classEntity = new ClassEntity();
        classEntity.setId(1L);
        classEntity.setName("Math 101");

        classDto = new ClassDto(
                1L,  // id
                "Math 101",  // name
                null,  // yearOfStudy
                null,  // maxStudents
                Collections.emptySet(),  // studentIds
                Collections.emptySet(),  // courseIds
                Collections.emptySet(),  // teacherIds
                null  // assignedRoomId
        );
    }

    // =================== CREATE TESTS ===================

    @Test
    void create_withValidRequest_createsClass() {
        // given
        CreateClassRequest request = new CreateClassRequest("Physics 201");
        given(classRepo.existsByNameIgnoreCase("Physics 201")).willReturn(false);
        given(classRepo.save(any(ClassEntity.class))).willReturn(classEntity);
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.create(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(classRepo).save(any(ClassEntity.class));
        verify(mapper).toClassDto(classEntity);
    }

    @Test
    void create_withDuplicateName_throwsConflictException() {
        // given
        CreateClassRequest request = new CreateClassRequest("Math 101");
        given(classRepo.existsByNameIgnoreCase("Math 101")).willReturn(true);

        // when/then
        assertThatThrownBy(() -> classService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Class name already exists");
        
        verify(classRepo, never()).save(any());
    }

    @Test
    void create_withEmptyName_handlesGracefully() {
        // given
        CreateClassRequest request = new CreateClassRequest("");
        given(classRepo.existsByNameIgnoreCase("")).willReturn(false);
        given(classRepo.save(any(ClassEntity.class))).willReturn(classEntity);
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.create(request);

        // then
        assertThat(result).isNotNull();
        verify(classRepo).save(any(ClassEntity.class));
    }

    // =================== UPDATE TESTS ===================

    @Test
    void update_withValidRequest_updatesClass() {
        // given
        UpdateClassRequest request = new UpdateClassRequest("Updated Math 101");
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.update(1L, request);

        // then
        assertThat(result).isNotNull();
        verify(mapper).updateClassEntity(request, classEntity);
        verify(mapper).toClassDto(classEntity);
    }

    @Test
    void update_withInvalidId_throwsResourceNotFoundException() {
        // given
        UpdateClassRequest request = new UpdateClassRequest("Updated Name");
        given(classRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> classService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(mapper, never()).updateClassEntity(any(), any());
    }

    // =================== DELETE TESTS ===================

    @Test
    void delete_withValidId_deletesClass() {
        // given
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        willDoNothing().given(classRepo).deleteById(1L);

        // when
        classService.delete(1L);

        // then
        verify(classRepo).deleteById(1L);
    }

    @Test
    void delete_withInvalidId_throwsResourceNotFoundException() {
        // given
        given(classRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> classService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(classRepo, never()).deleteById(any());
    }

    // =================== GET TESTS ===================

    @Test
    void get_withValidId_returnsClass() {
        // given
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.get(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Math 101");
    }

    @Test
    void get_withInvalidId_throwsResourceNotFoundException() {
        // given
        given(classRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> classService.get(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =================== LIST TESTS ===================

    @Test
    @SuppressWarnings("unchecked")
    void list_withoutFilter_returnsAllClasses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ClassEntity> classes = Arrays.asList(classEntity);
        Page<ClassEntity> page = new PageImpl<>(classes, pageable, 1);
        
        given(classRepo.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        Page<ClassDto> result = classService.list(pageable, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Math 101");
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withNameFilter_returnsFilteredClasses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ClassEntity> classes = Arrays.asList(classEntity);
        Page<ClassEntity> page = new PageImpl<>(classes, pageable, 1);
        
        given(classRepo.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        Page<ClassDto> result = classService.list(pageable, "Math");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withEmptyNameFilter_returnsAllClasses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ClassEntity> classes = Arrays.asList(classEntity);
        Page<ClassEntity> page = new PageImpl<>(classes, pageable, 1);
        
        given(classRepo.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        Page<ClassDto> result = classService.list(pageable, "");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    // =================== STUDENT MUTATION TESTS ===================

    @Test
    void addStudent_withValidIds_addsStudentToClass() {
        // given
        Student student = new Student();
        student.setId(10L);
        
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(studentRepo.findById(10L)).willReturn(Optional.of(student));
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.addStudent(1L, 10L);

        // then
        assertThat(result).isNotNull();
        verify(classRepo).findById(1L);
        verify(studentRepo).findById(10L);
    }

    @Test
    void addStudent_withInvalidClassId_throwsResourceNotFoundException() {
        // given
        given(classRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> classService.addStudent(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addStudent_withInvalidStudentId_throwsResourceNotFoundException() {
        // given
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(studentRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> classService.addStudent(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeStudent_withValidIds_removesStudentFromClass() {
        // given
        Student student = new Student();
        student.setId(10L);
        classEntity.setStudents(new HashSet<>(Arrays.asList(student)));
        
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.removeStudent(1L, 10L);

        // then
        assertThat(result).isNotNull();
        verify(classRepo).findById(1L);
    }

    // =================== COURSE MUTATION TESTS ===================

    @Test
    void addCourse_withValidIds_addsCourseToClass() {
        // given
        Course course = new Course();
        course.setId(20L);
        
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(courseRepo.findById(20L)).willReturn(Optional.of(course));
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.addCourse(1L, 20L);

        // then
        assertThat(result).isNotNull();
        verify(courseRepo).findById(20L);
    }

    @Test
    void addCourse_withInvalidCourseId_throwsResourceNotFoundException() {
        // given
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(courseRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> classService.addCourse(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeCourse_withValidIds_removesCourseFromClass() {
        // given
        Course course = new Course();
        course.setId(20L);
        classEntity.setCourses(new HashSet<>(Arrays.asList(course)));
        
        given(classRepo.findById(1L)).willReturn(Optional.of(classEntity));
        given(mapper.toClassDto(classEntity)).willReturn(classDto);

        // when
        ClassDto result = classService.removeCourse(1L, 20L);

        // then
        assertThat(result).isNotNull();
        verify(classRepo).findById(1L);
    }
}