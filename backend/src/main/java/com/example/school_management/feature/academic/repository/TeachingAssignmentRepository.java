package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.dto.ClassCountRow;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeachingAssignmentRepository
        extends JpaRepository<TeachingAssignment, Long> {

    /* Count teachers & courses for many classes at once ---------- */
    @Query("""
       SELECT
           c.id                      AS classId,
           COUNT(DISTINCT ta.id)     AS teacherCnt,
           COUNT(DISTINCT cc.id)     AS courseCnt,
           COUNT(DISTINCT cs.id)     AS studentCnt
       FROM   ClassEntity     c
       LEFT JOIN c.courses    cc
       LEFT JOIN c.students   cs
       LEFT JOIN TeachingAssignment ta
                ON ta.clazz.id = c.id
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
         ORDER BY c.name
    """)
    List<TeachingAssignment> findAllByClassId(@Param("classId") Long classId);

    boolean existsByClazzIdAndCourseId(Long classId, Long courseId);
}
