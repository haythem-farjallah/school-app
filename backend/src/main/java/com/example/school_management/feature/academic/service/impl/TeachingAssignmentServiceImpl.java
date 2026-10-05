package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.commons.service.GenericFilterService;
import com.example.school_management.commons.utils.FilterFields;
import com.example.school_management.feature.academic.dto.CreateTeachingAssignmentDto;
import com.example.school_management.feature.academic.dto.UpdateTeachingAssignmentDto;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.mapper.TeachingAssignmentMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.service.TeachingAssignmentService;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TeachingAssignmentServiceImpl implements TeachingAssignmentService {

    /** Paths accepted by GET /admin/teaching-assignments and its /filter. */
    private static final FilterFields FILTER_FIELDS = new FilterFields(
            Set.of("teacher.firstName", "teacher.lastName", "teacher.email",
                    "course.name", "course.code", "clazz.name", "weeklyHours"),
            Set.of("teacher.lastName", "course.name", "clazz.name", "weeklyHours"));

    private final TeachingAssignmentRepository assignmentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final TeachingAssignmentMapper mapper;
    private final GenericFilterService genericFilterService;
    private final CurrentSchoolResolver currentSchool;

    private Teacher requireSchoolTeacher(Long id) {
        return teacherRepository.findByIdAndSchoolId(id, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }

    private Course requireSchoolCourse(Long id) {
        return courseRepository.findByIdAndSchoolId(id, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private ClassEntity requireSchoolClass(Long id) {
        return classRepository.findByIdAndAcademicYearSchoolId(id, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }

    private Specification<TeachingAssignment> schoolSpec() {
        Long schoolId = currentSchool.resolve().getId();
        return (root, query, cb) -> {
            var membershipQuery = query.subquery(Long.class);
            var membership = membershipQuery.from(SchoolMembership.class);
            membershipQuery.select(membership.get("id")).where(
                    cb.equal(membership.get("user").get("id"), root.get("teacher").get("id")),
                    cb.equal(membership.get("school").get("id"), schoolId),
                    cb.isMember(MembershipRole.TEACHER, membership.get("roles")));
            return cb.and(
                    cb.equal(root.get("clazz").get("academicYear").get("school").get("id"), schoolId),
                    cb.equal(root.get("course").get("school").get("id"), schoolId),
                    cb.exists(membershipQuery));
        };
    }

    @Override
    public TeachingAssignment create(CreateTeachingAssignmentDto dto) {
        Teacher teacher = requireSchoolTeacher(dto.teacherId());
        Course course = requireSchoolCourse(dto.courseId());
        ClassEntity clazz = requireSchoolClass(dto.classId());
        // The existing unique Class/Course model remains unchanged. Both IDs are validated before this check.
        if (assignmentRepository.existsByClazzIdAndCourseId(dto.classId(), dto.courseId())) {
            throw new ConflictException("Assignment already exists for course " + course.getName() + " in class " + clazz.getName());
        }
        TeachingAssignment assignment = mapper.toEntity(dto);
        assignment.setTeacher(teacher);
        assignment.setCourse(course);
        assignment.setClazz(clazz);
        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingAssignment find(long id) {
        Specification<TeachingAssignment> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
        return assignmentRepository.findOne(schoolSpec().and(idSpec))
                .orElseThrow(() -> new ResourceNotFoundException("Teaching assignment not found with id: " + id));
    }

    @Override
    public TeachingAssignment patch(long id, UpdateTeachingAssignmentDto dto) {
        TeachingAssignment assignment = find(id);
        // Resolve every replacement before changing the managed entity.
        Teacher teacher = dto.teacherId() != null ? requireSchoolTeacher(dto.teacherId()) : assignment.getTeacher();
        Course course = dto.courseId() != null ? requireSchoolCourse(dto.courseId()) : assignment.getCourse();
        ClassEntity clazz = dto.classId() != null ? requireSchoolClass(dto.classId()) : assignment.getClazz();
        assignment.setTeacher(teacher);
        assignment.setCourse(course);
        assignment.setClazz(clazz);
        if (dto.weeklyHours() != null) assignment.setWeeklyHours(dto.weeklyHours());
        return assignmentRepository.save(assignment);
    }

    @Override
    public void delete(long id) {
        assignmentRepository.delete(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findAll(Pageable pageable) {
        return assignmentRepository.findAll(schoolSpec(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) return findAll(pageable);
        String pattern = "%" + query.toLowerCase(java.util.Locale.ROOT) + "%";
        Specification<TeachingAssignment> spec = (root, criteriaQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("teacher").get("firstName")), pattern),
                cb.like(cb.lower(root.get("teacher").get("lastName")), pattern),
                cb.like(cb.lower(root.get("teacher").get("email")), pattern),
                cb.like(cb.lower(root.get("course").get("name")), pattern),
                cb.like(cb.lower(root.get("course").get("code")), pattern),
                cb.like(cb.lower(root.get("clazz").get("name")), pattern));
        return assignmentRepository.findAll(schoolSpec().and(spec), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        Specification<TeachingAssignment> spec = genericFilterService.buildSpecificationFromParams(parameterMap, FILTER_FIELDS);
        return assignmentRepository.findAll(schoolSpec().and(spec), pageable);
    }

    @Override
    public void bulkDelete(List<Long> ids) {
        List<TeachingAssignment> validated = findByIds(ids);
        assignmentRepository.deleteAll(validated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByIds(List<Long> ids) {
        return ids.stream().map(this::find).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findAll() {
        return assignmentRepository.findAll(schoolSpec());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByTeacherId(Long teacherId) {
        requireSchoolTeacher(teacherId);
        return assignmentRepository.findByTeacherIdAndSchoolId(teacherId, currentSchool.resolve().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findByTeacherId(Long teacherId, Pageable pageable) {
        requireSchoolTeacher(teacherId);
        Specification<TeachingAssignment> spec = (root, query, cb) -> cb.equal(root.get("teacher").get("id"), teacherId);
        return assignmentRepository.findAll(schoolSpec().and(spec), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByCourseId(Long courseId) {
        requireSchoolCourse(courseId);
        Specification<TeachingAssignment> spec = (root, query, cb) -> cb.equal(root.get("course").get("id"), courseId);
        return assignmentRepository.findAll(schoolSpec().and(spec));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findByCourseId(Long courseId, Pageable pageable) {
        requireSchoolCourse(courseId);
        Specification<TeachingAssignment> spec = (root, query, cb) -> cb.equal(root.get("course").get("id"), courseId);
        return assignmentRepository.findAll(schoolSpec().and(spec), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByClassId(Long classId) {
        requireSchoolClass(classId);
        Specification<TeachingAssignment> spec = (root, query, cb) -> cb.equal(root.get("clazz").get("id"), classId);
        return assignmentRepository.findAll(schoolSpec().and(spec));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findByClassId(Long classId, Pageable pageable) {
        requireSchoolClass(classId);
        Specification<TeachingAssignment> spec = (root, query, cb) -> cb.equal(root.get("clazz").get("id"), classId);
        return assignmentRepository.findAll(schoolSpec().and(spec), pageable);
    }

    @Override
    public void assignTeacherToCourses(Long teacherId, List<Long> courseIds, Long classId) {
        Teacher teacher = requireSchoolTeacher(teacherId);
        ClassEntity clazz = requireSchoolClass(classId);
        List<Course> courses = courseIds.stream().map(this::requireSchoolCourse).toList();
        for (Course course : courses) {
            if (!assignmentRepository.existsByClazzIdAndCourseId(classId, course.getId())) {
                TeachingAssignment assignment = new TeachingAssignment();
                assignment.setTeacher(teacher);
                assignment.setCourse(course);
                assignment.setClazz(clazz);
                assignment.setWeeklyHours(0);
                assignmentRepository.save(assignment);
            }
        }
    }

    @Override
    public void assignTeachersToClass(List<Long> teacherIds, Long classId, Long courseId) {
        Course course = requireSchoolCourse(courseId);
        ClassEntity clazz = requireSchoolClass(classId);
        List<Teacher> teachers = teacherIds.stream().map(this::requireSchoolTeacher).toList();
        for (Teacher teacher : teachers) {
            if (!assignmentRepository.existsByClazzIdAndCourseId(classId, courseId)) {
                TeachingAssignment assignment = new TeachingAssignment();
                assignment.setTeacher(teacher);
                assignment.setCourse(course);
                assignment.setClazz(clazz);
                assignment.setWeeklyHours(0);
                assignmentRepository.save(assignment);
            }
        }
    }

    @Override
    public void bulkAssignTeachersToCourses(List<CreateTeachingAssignmentDto> assignments) {
        // Tenant invalidity is atomic even though existing non-tenant failures remain best effort.
        for (CreateTeachingAssignmentDto dto : assignments) {
            requireSchoolTeacher(dto.teacherId());
            requireSchoolClass(dto.classId());
            requireSchoolCourse(dto.courseId());
        }
        for (CreateTeachingAssignmentDto dto : assignments) {
            try {
                create(dto);
            } catch (Exception e) {
                log.warn("Failed to create teaching assignment for class {} and course {}", dto.classId(), dto.courseId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTeacherAndCourseAndClass(Long teacherId, Long courseId, Long classId) {
        requireSchoolTeacher(teacherId);
        requireSchoolCourse(courseId);
        requireSchoolClass(classId);
        Specification<TeachingAssignment> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("teacher").get("id"), teacherId),
                cb.equal(root.get("course").get("id"), courseId),
                cb.equal(root.get("clazz").get("id"), classId));
        return assignmentRepository.exists(schoolSpec().and(spec));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConflictingAssignment(Long teacherId, Long courseId, Long classId) {
        requireSchoolClass(classId);
        requireSchoolCourse(courseId);
        // Class/Course uniqueness applies even to an existing inconsistent Teacher link.
        return assignmentRepository.existsByClazzIdAndCourseId(classId, courseId);
    }
}
