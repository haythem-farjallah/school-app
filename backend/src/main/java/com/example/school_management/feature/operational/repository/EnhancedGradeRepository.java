package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.EnhancedGrade;
import com.example.school_management.feature.operational.dto.CreateEnhancedGradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnhancedGradeRepository extends JpaRepository<EnhancedGrade, Long> {
    
    // Find grades by student
    List<EnhancedGrade> findByStudentId(Long studentId);
    List<EnhancedGrade> findByStudentIdAndSemester(Long studentId, CreateEnhancedGradeRequest.Semester semester);
    
    // Find grades by teacher
    List<EnhancedGrade> findByTeacherId(Long teacherId);
    List<EnhancedGrade> findByTeacherIdAndClassIdAndCourseId(Long teacherId, Long classId, Long courseId);
    
    // Find grades by class
    List<EnhancedGrade> findByClassId(Long classId);
    List<EnhancedGrade> findByClassIdAndSemester(Long classId, CreateEnhancedGradeRequest.Semester semester);
    
    // Find grades by course
    List<EnhancedGrade> findByCourseId(Long courseId);
    
    // Find grades by student and course
    List<EnhancedGrade> findByStudentIdAndCourseId(Long studentId, Long courseId);
    
    // Find specific grade
    Optional<EnhancedGrade> findByStudentIdAndCourseIdAndExamTypeAndSemester(
            Long studentId, Long courseId, CreateEnhancedGradeRequest.ExamType examType, CreateEnhancedGradeRequest.Semester semester);
    
    // Find grades for teacher's classes
    @Query("SELECT DISTINCT eg.classId FROM EnhancedGrade eg WHERE eg.teacherId = :teacherId")
    List<Long> findDistinctClassIdsByTeacherId(@Param("teacherId") Long teacherId);
    
    @Query("SELECT DISTINCT eg.courseId FROM EnhancedGrade eg WHERE eg.teacherId = :teacherId AND eg.classId = :classId")
    List<Long> findDistinctCourseIdsByTeacherIdAndClassId(@Param("teacherId") Long teacherId, @Param("classId") Long classId);
    
    // Find students in teacher's class for a course
    @Query("SELECT DISTINCT eg.studentId FROM EnhancedGrade eg WHERE eg.teacherId = :teacherId AND eg.classId = :classId AND eg.courseId = :courseId")
    List<Long> findDistinctStudentIdsByTeacherIdAndClassIdAndCourseId(@Param("teacherId") Long teacherId, @Param("classId") Long classId, @Param("courseId") Long courseId);
    
    // Grade statistics queries
    @Query("SELECT AVG(eg.percentage) FROM EnhancedGrade eg WHERE eg.studentId = :studentId AND eg.semester = :semester")
    Double calculateStudentAverageForSemester(@Param("studentId") Long studentId, @Param("semester") CreateEnhancedGradeRequest.Semester semester);
    
    @Query("SELECT AVG(eg.percentage) FROM EnhancedGrade eg WHERE eg.classId = :classId AND eg.semester = :semester")
    Double calculateClassAverageForSemester(@Param("classId") Long classId, @Param("semester") CreateEnhancedGradeRequest.Semester semester);
    
    // Approval related queries
    List<EnhancedGrade> findByStudentIdAndSemesterAndIsApproved(Long studentId, CreateEnhancedGradeRequest.Semester semester, Boolean isApproved);
    List<EnhancedGrade> findByClassIdAndSemesterAndIsApproved(Long classId, CreateEnhancedGradeRequest.Semester semester, Boolean isApproved);
    
    @Query("SELECT COUNT(DISTINCT eg.studentId) FROM EnhancedGrade eg WHERE eg.classId = :classId AND eg.semester = :semester")
    Long countDistinctStudentsByClassIdAndSemester(@Param("classId") Long classId, @Param("semester") CreateEnhancedGradeRequest.Semester semester);
}
