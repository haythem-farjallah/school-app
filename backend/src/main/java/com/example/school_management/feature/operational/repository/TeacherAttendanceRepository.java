package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.TeacherAttendance;
import com.example.school_management.feature.operational.dto.TeacherAttendanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {
    
    // Find attendance by teacher and date
    Optional<TeacherAttendance> findByTeacherIdAndDate(Long teacherId, LocalDate date);
    
    // Find all attendance records for a specific date
    List<TeacherAttendance> findByDate(LocalDate date);
    
    // Find attendance records for a date range
    List<TeacherAttendance> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Find attendance records for a teacher in a date range
    List<TeacherAttendance> findByTeacherIdAndDateBetween(Long teacherId, LocalDate startDate, LocalDate endDate);
    
    // Find attendance records by teacher ID
    List<TeacherAttendance> findByTeacherId(Long teacherId);
    
    // Find attendance records by status
    List<TeacherAttendance> findByStatus(TeacherAttendanceRequest.TeacherAttendanceStatus status);
    
    // Find attendance records by status and date
    List<TeacherAttendance> findByStatusAndDate(TeacherAttendanceRequest.TeacherAttendanceStatus status, LocalDate date);
    
    // Custom query for attendance statistics
    @Query("SELECT COUNT(ta) FROM TeacherAttendance ta WHERE ta.teacherId = :teacherId AND ta.status = :status")
    Long countByTeacherIdAndStatus(@Param("teacherId") Long teacherId, @Param("status") TeacherAttendanceRequest.TeacherAttendanceStatus status);
    
    @Query("SELECT COUNT(ta) FROM TeacherAttendance ta WHERE ta.teacherId = :teacherId AND ta.date BETWEEN :startDate AND :endDate")
    Long countByTeacherIdAndDateBetween(@Param("teacherId") Long teacherId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(ta) FROM TeacherAttendance ta WHERE ta.teacherId = :teacherId AND ta.status = :status AND ta.date BETWEEN :startDate AND :endDate")
    Long countByTeacherIdAndStatusAndDateBetween(@Param("teacherId") Long teacherId, @Param("status") TeacherAttendanceRequest.TeacherAttendanceStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Monthly statistics query
    @Query("SELECT EXTRACT(MONTH FROM ta.date) as month, EXTRACT(YEAR FROM ta.date) as year, " +
           "COUNT(CASE WHEN ta.status = 'PRESENT' THEN 1 END) as present, " +
           "COUNT(CASE WHEN ta.status != 'PRESENT' THEN 1 END) as absent " +
           "FROM TeacherAttendance ta WHERE ta.teacherId = :teacherId " +
           "GROUP BY EXTRACT(MONTH FROM ta.date), EXTRACT(YEAR FROM ta.date) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyStatistics(@Param("teacherId") Long teacherId);
}
