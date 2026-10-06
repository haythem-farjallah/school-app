package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.ClassEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> , JpaSpecificationExecutor<ClassEntity> {
    List<ClassEntity> findByAcademicYearSchoolId(Long schoolId);

    @Query("SELECT COUNT(c) FROM ClassEntity c WHERE c.academicYear.school.id = :schoolId")
    long countBySchoolId(@Param("schoolId") Long schoolId);
    boolean existsByAcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);
    boolean existsByAcademicYearIdAndNameIgnoreCaseAndIdNot(Long academicYearId, String name, Long id);
    Optional<ClassEntity> findByIdAndAcademicYearSchoolId(Long id, Long schoolId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassEntity c WHERE c.id = :id AND c.academicYear.school.id = :schoolId")
    Optional<ClassEntity> findSchoolClassForUpdate(@Param("id") Long id, @Param("schoolId") Long schoolId);

    Optional<ClassEntity> findByAcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);

    boolean existsByName(String name);
    List<ClassEntity> findByAcademicYearId(Long academicYearId);
    
    // Count classes by teacher ID (through multiple relationships)
    @Query("""
        SELECT COUNT(DISTINCT c) FROM ClassEntity c 
        WHERE c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId
            UNION  
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
        )
        """)
    Long countByTeacherId(@Param("teacherId") Long teacherId);
    
    // Find classes by teacher ID (through multiple relationships - TeachingAssignment, TimetableSlot, or direct relationship)
    @Query("""
        SELECT DISTINCT c FROM ClassEntity c 
        LEFT JOIN FETCH c.assignedRoom
        WHERE c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId
            UNION  
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
        )
        ORDER BY c.name
        """)
    List<ClassEntity> findByTeacherId(@Param("teacherId") Long teacherId);

    // One ownership predicate shared by content, count and presentation-search queries.
    String TEACHER_CLASS_SCOPE = """
        WHERE c.academicYear.school.id = :schoolId
          AND EXISTS (SELECT m.id FROM SchoolMembership m JOIN m.roles role
              WHERE m.user.id = :teacherId AND m.school.id = :schoolId
                AND role = com.example.school_management.feature.membership.entity.MembershipRole.TEACHER)
          AND c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
              AND ta.clazz.academicYear.school.id = :schoolId AND ta.course.school.id = :schoolId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts
              LEFT JOIN ts.forCourse slotCourse LEFT JOIN ts.room slotRoom
              LEFT JOIN ts.timetable slotTimetable
              WHERE ts.teacher.id = :teacherId
              AND ts.forClass.academicYear.school.id = :schoolId
              AND (slotTimetable IS NULL OR slotTimetable.school.id = :schoolId)
              AND ts.period.school.id = :schoolId
              AND (slotCourse IS NULL OR slotCourse.school.id = :schoolId)
              AND (slotRoom IS NULL OR slotRoom.school.id = :schoolId)
            UNION
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
              AND ct.academicYear.school.id = :schoolId
        )
        """;

    String TEACHER_CLASS_SEARCH = """
        AND (:search IS NULL
          OR LOWER(c.name) LIKE :search ESCAPE '\\'
          OR LOWER(c.gradeLevel) LIKE :search ESCAPE '\\'
          OR ((room IS NULL OR room.school.id = :schoolId)
              AND LOWER(COALESCE(room.name, 'TBD')) LIKE :search ESCAPE '\\')
          OR EXISTS (SELECT a.id FROM TeachingAssignment a
              WHERE a.clazz.id = c.id AND a.teacher.id = :teacherId AND a.course.school.id = :schoolId
                AND (LOWER(a.course.name) LIKE :search ESCAPE '\\'
                     OR LOWER(a.course.code) LIKE :search ESCAPE '\\'))
          OR (NOT EXISTS (SELECT a.id FROM TeachingAssignment a
                  WHERE a.clazz.id = c.id AND a.teacher.id = :teacherId AND a.course.school.id = :schoolId)
              AND EXISTS (SELECT course.id FROM ClassEntity legacy JOIN legacy.courses course
                  WHERE legacy.id = c.id AND course.school.id = :schoolId AND (LOWER(course.name) LIKE :search ESCAPE '\\'
                      OR LOWER(course.code) LIKE :search ESCAPE '\\'))))
        """;

    @Query("SELECT c FROM ClassEntity c JOIN FETCH c.academicYear LEFT JOIN FETCH c.schedule LEFT JOIN FETCH c.assignedRoom " + TEACHER_CLASS_SCOPE + " ORDER BY c.name, c.id")
    List<ClassEntity> findByTeacherIdAndSchoolId(@Param("teacherId") Long teacherId,
                                               @Param("schoolId") Long schoolId);

    @Query(value = "SELECT c FROM ClassEntity c JOIN FETCH c.academicYear LEFT JOIN FETCH c.schedule LEFT JOIN FETCH c.assignedRoom " + TEACHER_CLASS_SCOPE + " ORDER BY c.name, c.id",
           countQuery = "SELECT COUNT(c) FROM ClassEntity c " + TEACHER_CLASS_SCOPE)
    Page<ClassEntity> findPageByTeacherIdAndSchoolId(@Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId, Pageable pageable);

    @Query(value = "SELECT c FROM ClassEntity c JOIN FETCH c.academicYear LEFT JOIN FETCH c.schedule LEFT JOIN FETCH c.assignedRoom room " + TEACHER_CLASS_SCOPE + TEACHER_CLASS_SEARCH + " ORDER BY c.name, c.id",
           countQuery = "SELECT COUNT(c) FROM ClassEntity c LEFT JOIN c.assignedRoom room " + TEACHER_CLASS_SCOPE + TEACHER_CLASS_SEARCH)
    Page<ClassEntity> findTeacherClassPage(@Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId, @Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM ClassEntity c JOIN FETCH c.academicYear LEFT JOIN FETCH c.schedule LEFT JOIN FETCH c.assignedRoom room " + TEACHER_CLASS_SCOPE + TEACHER_CLASS_SEARCH + " ORDER BY c.name, c.id")
    List<ClassEntity> findTeacherClasses(@Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId, @Param("search") String search);

    // Collections are loaded only after selecting the page, never fetch-joined into pagination.
    @Query("SELECT DISTINCT c FROM ClassEntity c LEFT JOIN FETCH c.courses course LEFT JOIN FETCH course.teacher WHERE c.id IN :classIds AND c.academicYear.school.id = :schoolId")
    List<ClassEntity> findWithCoursesByIdsAndSchoolId(@Param("classIds") List<Long> classIds, @Param("schoolId") Long schoolId);

    @Query("SELECT DISTINCT c FROM ClassEntity c LEFT JOIN FETCH c.teachers WHERE c.id IN :classIds AND c.academicYear.school.id = :schoolId")
    List<ClassEntity> findWithTeachersByIdsAndSchoolId(@Param("classIds") List<Long> classIds, @Param("schoolId") Long schoolId);
}
