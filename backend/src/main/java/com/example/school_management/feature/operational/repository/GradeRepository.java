package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long>, JpaSpecificationExecutor<Grade> {
    
    // Legacy consumers outside Grade/Transcript workflows: TeacherClass and Dashboard.
    @Query("SELECT g FROM Grade g WHERE g.enrollment.classEntity.id = :classId")
    List<Grade> findByClassId(@Param("classId") Long classId);

    @Query("SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId ORDER BY g.gradedAt DESC")
    Page<Grade> findByStudentIdOrderByGradedAtDesc(@Param("studentId") Long studentId, Pageable pageable);

    // Ownership is derived through Enrollment -> Class -> AcademicYear -> School.
    @Query("""
        SELECT g FROM Grade g WHERE g.id = :id AND g.enrollment.classEntity.academicYear.school.id = :schoolId
        """)
    Optional<Grade> findByIdAndSchoolId(@Param("id") Long id,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.classEntity.academicYear.school.id = :schoolId
        """)
    Page<Grade> findBySchoolId(@Param("schoolId") Long schoolId,
            Pageable pageable);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId ORDER BY g.gradedAt ASC
        """)
    List<Grade> findByStudentIdAndSchoolId(@Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId ORDER BY g.gradedAt DESC
        """)
    Page<Grade> findByStudentIdAndSchoolId(@Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.classEntity.id = :classId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId
        """)
    List<Grade> findByClassIdAndSchoolId(@Param("classId") Long classId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.classEntity.id = :classId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId ORDER BY g.gradedAt DESC
        """)
    Page<Grade> findByClassIdAndSchoolId(@Param("classId") Long classId,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    @Query("""
        SELECT g FROM Grade g WHERE g.assignedBy.id = :teacherId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId
        """)
    List<Grade> findByTeacherIdAndSchoolId(@Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.id = :enrollmentId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId
        """)
    List<Grade> findByEnrollmentIdAndSchoolId(@Param("enrollmentId") Long enrollmentId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.id = :enrollmentId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId ORDER BY g.gradedAt DESC
        """)
    Page<Grade> findByEnrollmentIdAndSchoolId(@Param("enrollmentId") Long enrollmentId,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId AND g.enrollment.classEntity.id =
        :classId AND g.enrollment.classEntity.academicYear.school.id = :schoolId
        """)
    List<Grade> findByStudentIdAndClassIdAndSchoolId(@Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId AND g.gradedAt BETWEEN :startDate AND
        :endDate
        """)
    List<Grade> findByStudentIdAndDateRangeAndSchoolId(@Param("studentId") Long studentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId AND
        g.enrollment.classEntity.academicYear.school.id = :schoolId AND g.gradedAt >= :startDate AND
        g.gradedAt < :endExclusive ORDER BY g.gradedAt ASC
        """)
    List<Grade> findByStudentIdAndPeriodAndSchoolId(@Param("studentId") Long studentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("schoolId") Long schoolId);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.classEntity.academicYear.school.id = :schoolId AND
        (LOWER(g.content) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(g.enrollment.student.firstName) LIKE
        LOWER(CONCAT('%', :search, '%')) OR LOWER(g.enrollment.student.lastName) LIKE LOWER(CONCAT('%',
        :search, '%')))
        """)
    Page<Grade> findBySearchAndSchoolId(@Param("search") String search,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.classEntity.academicYear.school.id = :schoolId AND
        g.enrollment.classEntity.id IN (SELECT DISTINCT c.id FROM ClassEntity c JOIN c.courses course WHERE
        course.id = :courseId)
        """)
    Page<Grade> findByCourseIdAndSchoolId(@Param("courseId") Long courseId,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    @Query("""
        SELECT g FROM Grade g WHERE g.enrollment.classEntity.academicYear.school.id = :schoolId AND
        (LOWER(g.content) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(g.enrollment.student.firstName) LIKE
        LOWER(CONCAT('%', :search, '%')) OR LOWER(g.enrollment.student.lastName) LIKE LOWER(CONCAT('%',
        :search, '%'))) AND g.enrollment.classEntity.id IN (SELECT DISTINCT c.id FROM ClassEntity c JOIN
        c.courses course WHERE course.id = :courseId)
        """)
    Page<Grade> findBySearchAndCourseIdAndSchoolId(@Param("search") String search,
            @Param("courseId") Long courseId,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

}
