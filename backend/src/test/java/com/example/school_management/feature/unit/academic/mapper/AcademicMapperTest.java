package com.example.school_management.feature.unit.academic.mapper;

import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicMapperTest {

    private AcademicMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AcademicMapper.class);
    }

    // =================== CLASS MAPPING TESTS ===================

    @Test
    void toClassDto_mapsAllFields() {
        // given
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(1L);
        classEntity.setName("Math 101");

        Student student = new Student();
        student.setId(10L);
        
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        
        classEntity.setEnrollments(Set.of(enrollment));

        Course course = new Course();
        course.setId(20L);
        classEntity.setCourses(Set.of(course));

        Teacher teacher = new Teacher();
        teacher.setId(30L);
        classEntity.setTeachers(Set.of(teacher));

        // when
        ClassDto dto = mapper.toClassDto(classEntity);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Math 101");
        assertThat(dto.studentIds()).contains(10L);
        assertThat(dto.courseIds()).contains(20L);
    }

    @Test
    void toClassDto_withNullCollections_returnsEmptySets() {
        // given
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(1L);
        classEntity.setName("Empty Class");
        classEntity.setEnrollments(new HashSet<>());
        classEntity.setCourses(new HashSet<>());
        classEntity.setTeachers(new HashSet<>());

        // when
        ClassDto dto = mapper.toClassDto(classEntity);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.studentIds()).isEmpty();
        assertThat(dto.courseIds()).isEmpty();
    }

    @Test
    void toClassDto_withInactiveEnrollments_filtersOut() {
        // given
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(1L);
        classEntity.setName("Test Class");

        Student student1 = new Student();
        student1.setId(10L);
        
        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setStudent(student1);
        activeEnrollment.setStatus(EnrollmentStatus.ACTIVE);

        Student student2 = new Student();
        student2.setId(20L);
        
        Enrollment inactiveEnrollment = new Enrollment();
        inactiveEnrollment.setStudent(student2);
        inactiveEnrollment.setStatus(EnrollmentStatus.DROPPED);

        classEntity.setEnrollments(Set.of(activeEnrollment, inactiveEnrollment));

        // when
        ClassDto dto = mapper.toClassDto(classEntity);

        // then
        assertThat(dto.studentIds()).contains(10L);
        assertThat(dto.studentIds()).doesNotContain(20L);
    }

    // =================== COURSE MAPPING TESTS ===================

    @Test
    void toCourseDto_mapsAllFields() {
        // given
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setEmail("teacher@test.com");

        Course course = new Course();
        course.setId(1L);
        course.setName("Physics");
        course.setCode("PHYS101");
        course.setCredit(4.0f);
        course.setWeeklyCapacity(5);
        course.setColor("#FF5733");
        course.setTeacher(teacher);

        // when
        CourseDto dto = mapper.toCourseDto(course);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Physics");
        assertThat(dto.credit()).isEqualTo(4.0f);
        assertThat(dto.weeklyCapacity()).isEqualTo(5);
        assertThat(dto.color()).isEqualTo("#FF5733");
        assertThat(dto.teacherId()).isEqualTo(1L);
    }

    @Test
    void toCourseDto_withNullTeacher_handlesGracefully() {
        // given
        Course course = new Course();
        course.setId(2L);
        course.setName("Chemistry");
        course.setCode("CHEM101");
        course.setCredit(3.0f);
        course.setTeacher(null);

        // when
        CourseDto dto = mapper.toCourseDto(course);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.teacherId()).isNull();
    }

    // =================== UPDATE MAPPING TESTS ===================

    @Test
    void updateClassEntity_updatesOnlyNonNullFields() {
        // given
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Old Name");

        UpdateClassRequest request = new UpdateClassRequest("New Name");

        // when
        mapper.updateClassEntity(request, classEntity);

        // then
        assertThat(classEntity.getName()).isEqualTo("New Name");
    }

    @Test
    void updateClassEntity_withNullValues_keepsOriginal() {
        // given
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Original Name");

        UpdateClassRequest request = new UpdateClassRequest(null);

        // when
        mapper.updateClassEntity(request, classEntity);

        // then
        assertThat(classEntity.getName()).isEqualTo("Original Name");
    }

    @Test
    void updateCourseEntity_updatesOnlyNonNullFields() {
        // given
        Course course = new Course();
        course.setName("Old Course");
        course.setCredit(3.0f);
        course.setColor("#000000");

        UpdateCourseRequest request = new UpdateCourseRequest(
                "New Course",
                "#FFFFFF",
                4.0f,
                6,
                null
        );

        // when
        mapper.updateCourseEntity(request, course);

        // then
        assertThat(course.getName()).isEqualTo("New Course");
        assertThat(course.getCredit()).isEqualTo(4.0f);
        assertThat(course.getColor()).isEqualTo("#FFFFFF");
    }

    // =================== CARD DTO MAPPING TESTS ===================

    @Test
    void toCardDto_mapsAllCounts() {
        // given
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(1L);
        classEntity.setName("Statistics 101");

        long studentCount = 25L;
        long courseCount = 5L;
        long teacherCount = 3L;

        // when
        ClassCardDto dto = mapper.toCardDto(classEntity, studentCount, courseCount, teacherCount);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.studentCount()).isEqualTo(25);
        assertThat(dto.courseCount()).isEqualTo(5);
        assertThat(dto.teacherCount()).isEqualTo(3);
    }

    @Test
    void toCardDto_withZeroCounts_mapsCorrectly() {
        // given
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(2L);
        classEntity.setName("Empty Class");

        // when
        ClassCardDto dto = mapper.toCardDto(classEntity, 0L, 0L, 0L);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.studentCount()).isEqualTo(0);
        assertThat(dto.courseCount()).isEqualTo(0);
        assertThat(dto.teacherCount()).isEqualTo(0);
    }

    // =================== ASSIGNMENT DTO MAPPING TESTS ===================

    @Test
    void toAssignmentDto_mapsAllFields() {
        // given
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setFirstName("John");
        teacher.setLastName("Doe");

        Course course = new Course();
        course.setId(10L);
        course.setName("Calculus");

        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setTeacher(teacher);
        assignment.setCourse(course);
        assignment.setWeeklyHours(8);

        // when
        AssignmentDto dto = mapper.toAssignmentDto(assignment);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.courseId()).isEqualTo(10L);
        assertThat(dto.courseName()).isEqualTo("Calculus");
        assertThat(dto.teacherId()).isEqualTo(1L);
        assertThat(dto.teacherName()).isEqualTo("John Doe");
        assertThat(dto.weeklyHours()).isEqualTo(8);
    }

    // =================== HELPER METHOD TESTS ===================

    @Test
    void mapStudents_withValidStudents_returnsIds() {
        // given
        Student student1 = new Student();
        student1.setId(1L);
        Student student2 = new Student();
        student2.setId(2L);
        
        Set<Student> students = Set.of(student1, student2);

        // when
        Set<Long> ids = AcademicMapper.mapStudents(students);

        // then
        assertThat(ids).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void mapCourses_withValidCourses_returnsIds() {
        // given
        Course course1 = new Course();
        course1.setId(10L);
        Course course2 = new Course();
        course2.setId(20L);
        
        Set<Course> courses = Set.of(course1, course2);

        // when
        Set<Long> ids = AcademicMapper.mapCourses(courses);

        // then
        assertThat(ids).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void mapTeachers_withValidTeachers_returnsIds() {
        // given
        Teacher teacher1 = new Teacher();
        teacher1.setId(100L);
        Teacher teacher2 = new Teacher();
        teacher2.setId(200L);
        
        Set<Teacher> teachers = Set.of(teacher1, teacher2);

        // when
        Set<Long> ids = AcademicMapper.mapTeachers(teachers);

        // then
        assertThat(ids).containsExactlyInAnyOrder(100L, 200L);
    }
}