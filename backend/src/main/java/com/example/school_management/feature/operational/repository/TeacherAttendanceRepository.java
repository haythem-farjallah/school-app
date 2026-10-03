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
    Optional<TeacherAttendance> findByIdAndSchoolId(Long id, Long schoolId);
    Optional<TeacherAttendance> findByTeacherIdAndDateAndSchoolId(Long teacherId, LocalDate date, Long schoolId);
    List<TeacherAttendance> findBySchoolId(Long schoolId);
    List<TeacherAttendance> findByDateAndSchoolId(LocalDate date, Long schoolId);
    List<TeacherAttendance> findByDateBetweenAndSchoolId(LocalDate startDate, LocalDate endDate, Long schoolId);
    List<TeacherAttendance> findByTeacherIdAndDateBetweenAndSchoolId(Long teacherId, LocalDate startDate,
                                                                 LocalDate endDate, Long schoolId);
    List<TeacherAttendance> findByTeacherIdAndSchoolId(Long teacherId, Long schoolId);
    Long countByTeacherIdAndDateBetweenAndSchoolId(Long teacherId, LocalDate startDate,
                                                 LocalDate endDate, Long schoolId);
    Long countByTeacherIdAndStatusAndDateBetweenAndSchoolId(Long teacherId,
            TeacherAttendanceRequest.TeacherAttendanceStatus status, LocalDate startDate,
            LocalDate endDate, Long schoolId);

    @Query("SELECT EXTRACT(MONTH FROM ta.date) as month, EXTRACT(YEAR FROM ta.date) as year, " +
           "COUNT(CASE WHEN ta.status = 'PRESENT' THEN 1 END) as present, " +
           "COUNT(CASE WHEN ta.status != 'PRESENT' THEN 1 END) as absent " +
           "FROM TeacherAttendance ta WHERE ta.teacherId = :teacherId AND ta.school.id = :schoolId " +
           "GROUP BY EXTRACT(MONTH FROM ta.date), EXTRACT(YEAR FROM ta.date) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyStatisticsByTeacherIdAndSchoolId(@Param("teacherId") Long teacherId,
                                                            @Param("schoolId") Long schoolId);
}
