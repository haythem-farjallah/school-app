package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    // Find all attendance records in date range
    @Query("SELECT a FROM Attendance a WHERE a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC")
    List<Attendance> findByDateBetween(@Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);

    // Find attendance by user and date range
    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC")
    List<Attendance> findByUserIdAndDateBetween(@Param("userId") Long userId, 
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);

    // Find attendance by class and date
    @Query("SELECT a FROM Attendance a WHERE a.classId = :classId AND a.date = :date ORDER BY a.user.firstName")
    List<Attendance> findByClassIdAndDate(@Param("classId") Long classId, @Param("date") LocalDate date);

    // Find attendance by course and date
    @Query("SELECT a FROM Attendance a WHERE a.course.id = :courseId AND a.date = :date ORDER BY a.user.firstName")
    List<Attendance> findByCourseIdAndDate(@Param("courseId") Long courseId, @Param("date") LocalDate date);

    // Find attendance by user type and date
    @Query("SELECT a FROM Attendance a WHERE a.userType = :userType AND a.date = :date ORDER BY a.user.firstName")
    List<Attendance> findByUserTypeAndDate(@Param("userType") UserType userType, @Param("date") LocalDate date);

    // Find attendance by user, course, and date
    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.course.id = :courseId AND a.date = :date")
    Optional<Attendance> findByUserIdAndCourseIdAndDate(@Param("userId") Long userId, 
                                                       @Param("courseId") Long courseId, 
                                                       @Param("date") LocalDate date);

    // Find attendance by user, class, and date
    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.classId = :classId AND a.date = :date")
    Optional<Attendance> findByUserIdAndClassIdAndDate(@Param("userId") Long userId, 
                                                      @Param("classId") Long classId, 
                                                      @Param("date") LocalDate date);

    // Count attendance by status for a user in date range
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.user.id = :userId AND a.status = :status AND a.date BETWEEN :startDate AND :endDate")
    Long countByUserIdAndStatusAndDateBetween(@Param("userId") Long userId, 
                                             @Param("status") AttendanceStatus status,
                                             @Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate);

    // Get attendance statistics for a class
    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.classId = :classId AND a.date BETWEEN :startDate AND :endDate GROUP BY a.status")
    List<Object[]> getAttendanceStatisticsByClass(@Param("classId") Long classId, 
                                                 @Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate);

    // Find all attendance records for a specific timetable slot
    @Query("SELECT a FROM Attendance a WHERE a.timetableSlot.id = :slotId AND a.date = :date")
    List<Attendance> findByTimetableSlotIdAndDate(@Param("slotId") Long slotId, @Param("date") LocalDate date);

    // Find attendance by user type and date range with pagination
    @Query("SELECT a FROM Attendance a WHERE a.userType = :userType AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC, a.user.firstName")
    Page<Attendance> findByUserTypeAndDateBetween(@Param("userType") UserType userType, 
                                                 @Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate, 
                                                 Pageable pageable);
    
    // Teacher-specific queries
    
    // Find attendance records for students in a teacher's classes on a specific date
    @Query("SELECT a FROM Attendance a WHERE a.timetableSlot.teacher.id = :teacherId AND a.date = :date AND a.userType = 'STUDENT' ORDER BY a.timetableSlot.period.index, a.user.firstName")
    List<Attendance> findStudentAttendanceByTeacherAndDate(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);
    
    // Find attendance for students in a specific timetable slot
    @Query("SELECT a FROM Attendance a WHERE a.timetableSlot.id = :slotId AND a.date = :date AND a.userType = 'STUDENT' ORDER BY a.user.firstName")
    List<Attendance> findStudentAttendanceBySlotAndDate(@Param("slotId") Long slotId, @Param("date") LocalDate date);
    
    // Check if attendance exists for a timetable slot on a date
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.timetableSlot.id = :slotId AND a.date = :date")
    boolean existsByTimetableSlotAndDate(@Param("slotId") Long slotId, @Param("date") LocalDate date);
    
    // Get absent students for a teacher on a specific date
    @Query("SELECT a FROM Attendance a WHERE a.timetableSlot.teacher.id = :teacherId AND a.date = :date AND a.status = 'ABSENT' AND a.userType = 'STUDENT' ORDER BY a.timetableSlot.period.index, a.user.firstName")
    List<Attendance> findAbsentStudentsByTeacherAndDate(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);
    
    // Get attendance summary for a teacher's classes in a date range
    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.timetableSlot.teacher.id = :teacherId AND a.date BETWEEN :startDate AND :endDate AND a.userType = 'STUDENT' GROUP BY a.status")
    List<Object[]> getAttendanceStatisticsByTeacher(@Param("teacherId") Long teacherId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
} 