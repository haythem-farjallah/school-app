package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.dto.ClassCountRow;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeachingAssignmentRepository
        extends JpaRepository<TeachingAssignment, Long>, JpaSpecificationExecutor<TeachingAssignment> {

    /* Count teachers & courses for many classes at once ---------- */
    @Query("""
       SELECT
           c.id                      AS classId,
           COUNT(DISTINCT ta.id)     AS teacherCnt,
           COUNT(DISTINCT cc.id)     AS courseCnt
       FROM   ClassEntity     c
       LEFT JOIN c.courses    cc
       LEFT JOIN TeachingAssignment ta
                ON ta.clazz.id = c.id
                   AND ta.course.school.id = c.academicYear.school.id
                   AND EXISTS (SELECT m.id FROM SchoolMembership m JOIN m.roles role
                       WHERE m.user.id = ta.teacher.id AND m.school.id = c.academicYear.school.id
                         AND role = com.example.school_management.feature.membership.entity.MembershipRole.TEACHER)
       WHERE  c.id IN :ids
       GROUP BY c.id
    """)
    List<ClassCountRow> aggregateForClasses(@Param("ids") List<Long> ids);


    /* All assignments for a single class (detail view) ----------- */
    @Query("""
        SELECT ta
          FROM TeachingAssignment ta
          JOIN FETCH ta.course c
          JOIN FETCH ta.teacher t
         WHERE ta.clazz.id = :classId
           AND c.school.id = ta.clazz.academicYear.school.id
           AND EXISTS (SELECT m.id FROM SchoolMembership m JOIN m.roles role
               WHERE m.user.id = t.id AND m.school.id = ta.clazz.academicYear.school.id
                 AND role = com.example.school_management.feature.membership.entity.MembershipRole.TEACHER)
         ORDER BY c.name
    """)
    List<TeachingAssignment> findAllByClassId(@Param("classId") Long classId);

    boolean existsByClazzIdAndCourseId(Long classId, Long courseId);
    
    /* Find assignments by teacher ID ----------- */
    @Query("""
        SELECT ta
        FROM TeachingAssignment ta
        JOIN FETCH ta.clazz c
        JOIN FETCH ta.course co
        JOIN FETCH ta.teacher t
        WHERE ta.teacher.id = :teacherId
        ORDER BY c.name, co.name
    """)
    List<TeachingAssignment> findByTeacherId(@Param("teacherId") Long teacherId);
    
    // Grade and Attendance consumers share the canonical Class/Course and Teacher membership boundary.
    @Query("""
        SELECT ta FROM TeachingAssignment ta
        JOIN FETCH ta.clazz c
        JOIN FETCH ta.course co
        LEFT JOIN FETCH co.teacher
        WHERE ta.teacher.id = :teacherId
          AND c.academicYear.school.id = :schoolId AND co.school.id = :schoolId
          AND EXISTS (SELECT m.id FROM SchoolMembership m JOIN m.roles role
              WHERE m.user.id = ta.teacher.id AND m.school.id = :schoolId
                AND role = com.example.school_management.feature.membership.entity.MembershipRole.TEACHER)
        ORDER BY c.name, co.name
    """)
    List<TeachingAssignment> findByTeacherIdAndSchoolId(@Param("teacherId") Long teacherId, @Param("schoolId") Long schoolId);

    // Additional query methods for teacher-course linking
    List<TeachingAssignment> findByCourseId(Long courseId);
    List<TeachingAssignment> findByClazzId(Long classId);
}
