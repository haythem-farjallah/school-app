package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
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
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByStudentId(@Param("studentId") Long studentId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.id = :classId ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByClassId(@Param("classId") Long classId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.status = :status ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByStatus(@Param("status") EnrollmentStatus status, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.classEntity.id = :classId")
    Optional<Enrollment> findByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = :status ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByClassIdAndStatus(@Param("classId") Long classId, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.enrolledAt BETWEEN :startDate AND :endDate ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByEnrolledAtBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate, 
                                            Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = :status")
    Long countActiveEnrollmentsByClassId(@Param("classId") Long classId, @Param("status") EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status")
    Long countActiveEnrollmentsByStudentId(@Param("studentId") Long studentId, @Param("status") EnrollmentStatus status);
    
    // Count all enrollments for a student (regardless of status)
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = :studentId")
    Long countByStudentId(@Param("studentId") Long studentId);
    
    // Find enrollments by student ID (without pagination)
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByStudentId(@Param("studentId") Long studentId);
    
    // Search enrollments by student name or class name
    @Query("SELECT e FROM Enrollment e WHERE " +
           "LOWER(e.student.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.student.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.classEntity.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findBySearch(@Param("search") String search, Pageable pageable);
    
    // Search enrollments by student name or class name and status
    @Query("SELECT e FROM Enrollment e WHERE " +
           "e.status = :status AND (" +
           "LOWER(e.student.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.student.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.classEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ") ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findBySearchAndStatus(@Param("search") String search, @Param("status") EnrollmentStatus status, Pageable pageable);
    
    // Get all enrollments for a student (regardless of status)
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findAllByStudentId(@Param("studentId") Long studentId);
    
    // Get all enrollments for a class (regardless of status)
    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.id = :classId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findAllByClassId(@Param("classId") Long classId);
} 