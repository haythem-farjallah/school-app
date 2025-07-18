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
    
    // Find grades by enrollment
    List<Grade> findByEnrollmentId(Long enrollmentId);
    
    // Find grades by student (through enrollment)
    @Query("SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId")
    List<Grade> findByStudentId(@Param("studentId") Long studentId);
    
    // Find grades by class (through enrollment)
    @Query("SELECT g FROM Grade g WHERE g.enrollment.classEntity.id = :classId")
    List<Grade> findByClassId(@Param("classId") Long classId);
    
    // Find grades by teacher
    @Query("SELECT g FROM Grade g WHERE g.assignedBy.id = :teacherId")
    List<Grade> findByTeacherId(@Param("teacherId") Long teacherId);
    
    // Find grades by student and class
    @Query("SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId AND g.enrollment.classEntity.id = :classId")
    List<Grade> findByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);
    
    // Find grades by class and teacher
    @Query("SELECT g FROM Grade g WHERE g.enrollment.classEntity.id = :classId AND g.assignedBy.id = :teacherId")
    List<Grade> findByClassIdAndTeacherId(@Param("classId") Long classId, @Param("teacherId") Long teacherId);
    
    // Find grades by date range
    @Query("SELECT g FROM Grade g WHERE g.gradedAt BETWEEN :startDate AND :endDate")
    List<Grade> findByGradedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Check if grade exists for enrollment and content to prevent duplicates
    @Query("SELECT g FROM Grade g WHERE g.enrollment.id = :enrollmentId AND g.content = :content")
    Optional<Grade> findByEnrollmentIdAndContent(@Param("enrollmentId") Long enrollmentId, @Param("content") String content);
    
    // Get average grade for student
    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.enrollment.student.id = :studentId")
    Double findAverageGradeByStudentId(@Param("studentId") Long studentId);
    
    // Get average grade for class
    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.enrollment.classEntity.id = :classId")
    Double findAverageGradeByClassId(@Param("classId") Long classId);
    
    // Get grade statistics for a student in a specific class
    @Query("SELECT AVG(g.score) as average, MIN(g.score) as minimum, MAX(g.score) as maximum, COUNT(g) as count " +
           "FROM Grade g WHERE g.enrollment.student.id = :studentId AND g.enrollment.classEntity.id = :classId")
    Object[] findGradeStatsByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);
    
    // Paginated queries
    Page<Grade> findByEnrollmentIdOrderByGradedAtDesc(Long enrollmentId, Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.enrollment.student.id = :studentId ORDER BY g.gradedAt DESC")
    Page<Grade> findByStudentIdOrderByGradedAtDesc(@Param("studentId") Long studentId, Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.enrollment.classEntity.id = :classId ORDER BY g.gradedAt DESC")
    Page<Grade> findByClassIdOrderByGradedAtDesc(@Param("classId") Long classId, Pageable pageable);
    
    // Search grades by content or student name
    @Query("SELECT g FROM Grade g WHERE " +
           "LOWER(g.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(g.enrollment.student.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(g.enrollment.student.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Grade> findBySearch(@Param("search") String search, Pageable pageable);
    
    // Find grades by course ID (simplified - we'll filter in service if needed)
    @Query("SELECT g FROM Grade g WHERE g.enrollment.classEntity.id IN " +
           "(SELECT DISTINCT c.id FROM ClassEntity c JOIN c.courses course WHERE course.id = :courseId)")
    Page<Grade> findByCourseId(@Param("courseId") Long courseId, Pageable pageable);
    
    // Find grades by search and course ID
    @Query("SELECT g FROM Grade g WHERE " +
           "(LOWER(g.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(g.enrollment.student.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(g.enrollment.student.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "g.enrollment.classEntity.id IN " +
           "(SELECT DISTINCT c.id FROM ClassEntity c JOIN c.courses course WHERE course.id = :courseId)")
    Page<Grade> findBySearchAndCourseId(@Param("search") String search, @Param("courseId") Long courseId, Pageable pageable);
} 