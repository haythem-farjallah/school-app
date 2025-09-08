/*package com.example.school_management.feature.unit.academic.services;

import com.example.school_management.feature.academic.dto.BatchIdsRequest;
import com.example.school_management.feature.academic.dto.ClassDto;
import com.example.school_management.feature.academic.dto.CreateClassRequest;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.service.ClassService;
import com.example.school_management.feature.academic.service.impl.ClassServiceImpl;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceImplTest {

    @Mock
    ClassRepository classRepo;

    @Mock
    CourseRepository courseRepo;
    @Mock
    StudentRepository studentRepo;
    @Mock
    TeacherRepository teacherRepo;
    @Mock
    TeachingAssignmentRepository assignmentRepo;
    @Mock
    AuditService auditService;
    @Mock
    BaseUserRepository<BaseUser> userRepo;

    AcademicMapper mapper = Mappers.getMapper(AcademicMapper.class);

    ClassService service;

    @BeforeEach
    void init() {
        service = new ClassServiceImpl(
                classRepo, courseRepo, studentRepo, teacherRepo, mapper, assignmentRepo, auditService, userRepo);
    }

    @Test
    void createClass_success() {
        CreateClassRequest req = new CreateClassRequest("3-A");

        ClassEntity saved = new ClassEntity(); saved.setId(1L); saved.setName("3-A");
        when(classRepo.existsByNameIgnoreCase("3-A")).thenReturn(false);
        when(classRepo.save(any(ClassEntity.class))).thenReturn(saved);

        ClassDto dto = service.create(req);

        assertThat(dto.id()).isEqualTo(1L);
        verify(classRepo).save(argThat(c -> "3-A".equals(c.getName())));
    }

    @Test
    void addCourse_toClass() {
        // given class 1 with empty set
        ClassEntity cls = new ClassEntity(); cls.setId(1L);

        when(classRepo.findById(1L)).thenReturn(Optional.of(cls));
        when(courseRepo.findById(17L)).thenReturn(Optional.of(new Course(){{
            setId(17L); setName("Math");
        }}));

        BatchIdsRequest addOne = new BatchIdsRequest(
                BatchIdsRequest.Operation.ADD, Set.of(17L));

        // when
        ClassDto dto = service.mutateCourses(1L, addOne);

        // then
        assertThat(dto.courseIds()).containsExactly(17L);
    }

    @Test
    void list_byNameLike() {
        ClassEntity cls = new ClassEntity(); cls.setId(3L); cls.setName("Science-A");
        when(classRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cls)));

        Page<ClassDto> p = service.list(PageRequest.of(0,10), "Sci");

        assertThat(p.getContent()).hasSize(1);
        verify(classRepo).findAll(any(Specification.class), eq(PageRequest.of(0,10)));
    }
}
*/