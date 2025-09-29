package com.example.school_management.feature.unit.academic.services;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.service.impl.CourseServiceImpl;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
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
class CourseServiceImplTest {

    @Mock CourseRepository courseRepo;
    @Mock TeacherRepository teacherRepo;
    @Mock AcademicMapper mapper;
    @Mock AuditService auditService;
    @Mock BaseUserRepository<BaseUser> userRepo;

    @InjectMocks CourseServiceImpl courseService;

    private Course course;
    private CourseDto courseDto;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setEmail("teacher@test.com");

        course = new Course();
        course.setId(1L);
        course.setName("Mathematics");
        course.setCode("MATH101");
        course.setCredit(3.0f);
        course.setColor("#FF5733");
        course.setWeeklyCapacity(4);
        course.setTeacher(teacher);

        courseDto = new CourseDto(1L, "Mathematics", "#FF5733", 3.0f, 4, 1L);
    }

    // =================== CREATE TESTS ===================

    @Test
    void create_withValidRequest_createsCourse() {
        // given
        CreateCourseRequest request = new CreateCourseRequest(
                "Physics", 
                "#0000FF", 
                4.0f, 
                5, 
                1L
        );
        
        given(courseRepo.existsByNameIgnoreCase("Physics")).willReturn(false);
        given(teacherRepo.findById(1L)).willReturn(Optional.of(teacher));
        given(courseRepo.countByCodeLike(anyString())).willReturn(0L);
        given(courseRepo.save(any(Course.class))).willReturn(course);
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        CourseDto result = courseService.create(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(courseRepo).save(any(Course.class));
    }

    @Test
    void create_withDuplicateName_throwsConflictException() {
        // given
        CreateCourseRequest request = new CreateCourseRequest(
                "Mathematics", 
                "#FF5733", 
                3.0f, 
                4, 
                1L
        );
        
        given(courseRepo.existsByNameIgnoreCase("Mathematics")).willReturn(true);

        // when/then
        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Course name already exists");
        
        verify(courseRepo, never()).save(any());
    }

    @Test
    void create_withNullTeacherId_createsWithoutTeacher() {
        // given
        CreateCourseRequest request = new CreateCourseRequest(
                "Chemistry", 
                "#00FF00", 
                3.0f, 
                4, 
                null
        );
        
        given(courseRepo.existsByNameIgnoreCase("Chemistry")).willReturn(false);
        given(courseRepo.countByCodeLike(anyString())).willReturn(0L);
        given(courseRepo.save(any(Course.class))).willReturn(course);
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        CourseDto result = courseService.create(request);

        // then
        assertThat(result).isNotNull();
        verify(teacherRepo, never()).findById(any());
    }

    @Test
    void create_withInvalidTeacherId_throwsResourceNotFoundException() {
        // given
        CreateCourseRequest request = new CreateCourseRequest(
                "Biology", 
                "#FF00FF", 
                3.0f, 
                4, 
                999L
        );
        
        given(courseRepo.existsByNameIgnoreCase("Biology")).willReturn(false);
        given(teacherRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =================== UPDATE TESTS ===================

    @Test
    void update_withValidRequest_updatesCourse() {
        // given
        UpdateCourseRequest request = new UpdateCourseRequest(
                "Updated Mathematics", 
                "#123456", 
                4.0f, 
                6, 
                1L
        );
        
        given(courseRepo.findById(1L)).willReturn(Optional.of(course));
        given(teacherRepo.findById(1L)).willReturn(Optional.of(teacher));
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        CourseDto result = courseService.update(1L, request);

        // then
        assertThat(result).isNotNull();
        verify(mapper).toCourseDto(course);
    }

    @Test
    void update_withInvalidId_throwsResourceNotFoundException() {
        // given
        UpdateCourseRequest request = new UpdateCourseRequest(
                "Updated Course", 
                "#FFFFFF", 
                3.0f, 
                4, 
                1L
        );
        
        given(courseRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> courseService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =================== DELETE TESTS ===================

    @Test
    void delete_withValidId_deletesCourse() {
        // given
        given(courseRepo.findById(1L)).willReturn(Optional.of(course));
        willDoNothing().given(courseRepo).deleteById(1L);

        // when
        courseService.delete(1L);

        // then
        verify(courseRepo).deleteById(1L);
    }

    @Test
    void delete_withInvalidId_throwsResourceNotFoundException() {
        // given
        given(courseRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> courseService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(courseRepo, never()).deleteById(any());
    }

    // =================== GET TESTS ===================

    @Test
    void get_withValidId_returnsCourse() {
        // given
        given(courseRepo.findById(1L)).willReturn(Optional.of(course));
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        CourseDto result = courseService.get(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Mathematics");
    }

    @Test
    void get_withInvalidId_throwsResourceNotFoundException() {
        // given
        given(courseRepo.findById(999L)).willReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> courseService.get(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =================== LIST TESTS ===================

    @Test
    @SuppressWarnings("unchecked")
    void list_withoutFilters_returnsAllCourses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Arrays.asList(course);
        Page<Course> page = new PageImpl<>(courses, pageable, 1);
        
        given(courseRepo.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        Page<CourseDto> result = courseService.list(pageable, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Mathematics");
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withTeacherIdFilter_returnsFilteredCourses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Arrays.asList(course);
        Page<Course> page = new PageImpl<>(courses, pageable, 1);
        
        given(courseRepo.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        Page<CourseDto> result = courseService.list(pageable, 1L, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withNameFilter_returnsFilteredCourses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Arrays.asList(course);
        Page<Course> page = new PageImpl<>(courses, pageable, 1);
        
        given(courseRepo.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        Page<CourseDto> result = courseService.list(pageable, null, "Math");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withBothFilters_returnsFilteredCourses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Arrays.asList(course);
        Page<Course> page = new PageImpl<>(courses, pageable, 1);
        
        given(courseRepo.findAll(any(Specification.class), eq(pageable))).willReturn(page);
        given(mapper.toCourseDto(course)).willReturn(courseDto);

        // when
        Page<CourseDto> result = courseService.list(pageable, 1L, "Math");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withEmptyResult_returnsEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Course> emptyPage = Page.empty(pageable);
        
        given(courseRepo.findAll(any(Specification.class), eq(pageable))).willReturn(emptyPage);

        // when
        Page<CourseDto> result = courseService.list(pageable, null, "NonExistent");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}