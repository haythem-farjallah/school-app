package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {
    @Query("SELECT MAX(a.date) FROM Attendance a WHERE a.school.id = :schoolId AND a.user.id = :studentId "
            + "AND a.classId = :classId AND a.course.id = :courseId AND a.userType = 'STUDENT'")
    LocalDate findLastStudentAttendanceDate(@Param("studentId") Long studentId, @Param("classId") Long classId,
            @Param("courseId") Long courseId, @Param("schoolId") Long schoolId);

    Optional<Attendance> findByIdAndSchoolId(Long id, Long schoolId);

    long countByUserIdAndStatusAndUserTypeAndSchoolId(Long userId, AttendanceStatus status, UserType userType, Long schoolId);

    boolean existsByUserIdAndSchoolId(Long userId, Long schoolId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC")
    List<Attendance> findByDateBetweenAndSchoolId(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate, @Param("schoolId") Long schoolId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.user.id = :userId AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC")
    List<Attendance> findByUserIdAndDateBetweenAndSchoolId(@Param("userId") Long userId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("schoolId") Long schoolId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.classId = :classId AND a.date = :date ORDER BY a.user.firstName")
    List<Attendance> findByClassIdAndDateAndSchoolId(@Param("classId") Long classId,
            @Param("date") LocalDate date, @Param("schoolId") Long schoolId);

    List<Attendance> findByClassIdAndDateBetweenAndSchoolId(Long classId, LocalDate startDate, LocalDate endDate, Long schoolId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.course.id = :courseId AND a.date = :date ORDER BY a.user.firstName")
    List<Attendance> findByCourseIdAndDateAndSchoolId(@Param("courseId") Long courseId,
            @Param("date") LocalDate date, @Param("schoolId") Long schoolId);

    Optional<Attendance> findByUserIdAndCourseIdAndDateAndSchoolId(Long userId, Long courseId, LocalDate date, Long schoolId);

    Optional<Attendance> findByUserIdAndClassIdAndDateAndSchoolId(Long userId, Long classId, LocalDate date, Long schoolId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.userType = :userType AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date DESC, a.user.firstName")
    Page<Attendance> findByUserTypeAndDateBetweenAndSchoolId(@Param("userType") UserType userType,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("schoolId") Long schoolId, Pageable pageable);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.timetableSlot.period.school.id = :schoolId AND a.timetableSlot.teacher.id = :teacherId AND a.date = :date AND a.userType = 'STUDENT' ORDER BY a.timetableSlot.period.index, a.user.firstName")
    List<Attendance> findStudentAttendanceByTeacherAndDateAndSchoolId(@Param("teacherId") Long teacherId,
            @Param("date") LocalDate date, @Param("schoolId") Long schoolId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.timetableSlot.id = :slotId AND a.date = :date AND a.userType = 'STUDENT' ORDER BY a.user.firstName")
    List<Attendance> findStudentAttendanceBySlotAndDateAndSchoolId(@Param("slotId") Long slotId,
            @Param("date") LocalDate date, @Param("schoolId") Long schoolId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId AND a.timetableSlot.period.school.id = :schoolId AND a.timetableSlot.teacher.id = :teacherId AND a.date = :date AND a.status = 'ABSENT' AND a.userType = 'STUDENT' ORDER BY a.timetableSlot.period.index, a.user.firstName")
    List<Attendance> findAbsentStudentsByTeacherAndDateAndSchoolId(@Param("teacherId") Long teacherId,
            @Param("date") LocalDate date, @Param("schoolId") Long schoolId);
}
