package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> , JpaSpecificationExecutor<ClassEntity> {
    List<ClassEntity> findByAcademicYearSchoolId(Long schoolId);

    @Query("SELECT COUNT(c) FROM ClassEntity c WHERE c.academicYear.school.id = :schoolId")
    long countBySchoolId(@Param("schoolId") Long schoolId);
    boolean existsByAcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);
    boolean existsByAcademicYearIdAndNameIgnoreCaseAndIdNot(Long academicYearId, String name, Long id);
    Optional<ClassEntity> findByIdAndAcademicYearSchoolId(Long id, Long schoolId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassEntity c WHERE c.id = :id AND c.academicYear.school.id = :schoolId")
    Optional<ClassEntity> findSchoolClassForUpdate(@Param("id") Long id, @Param("schoolId") Long schoolId);

    Optional<ClassEntity> findByAcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);

    boolean existsByName(String name);
    List<ClassEntity> findByAcademicYearId(Long academicYearId);
    
    // Count classes by teacher ID (through multiple relationships)
    @Query("""
        SELECT COUNT(DISTINCT c) FROM ClassEntity c 
        WHERE c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId
            UNION  
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
        )
        """)
    Long countByTeacherId(@Param("teacherId") Long teacherId);
    
    // Find classes by teacher ID (through multiple relationships - TeachingAssignment, TimetableSlot, or direct relationship)
    @Query("""
        SELECT DISTINCT c FROM ClassEntity c 
        LEFT JOIN FETCH c.assignedRoom
        WHERE c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId
            UNION  
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
        )
        ORDER BY c.name
        """)
    List<ClassEntity> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("""
        SELECT DISTINCT c FROM ClassEntity c
        LEFT JOIN FETCH c.assignedRoom
        WHERE c.academicYear.school.id = :schoolId
          AND EXISTS (SELECT m.id FROM SchoolMembership m JOIN m.roles role
              WHERE m.user.id = :teacherId AND m.school.id = :schoolId
                AND role = com.example.school_management.feature.membership.entity.MembershipRole.TEACHER)
          AND c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
              AND ta.clazz.academicYear.school.id = :schoolId AND ta.course.school.id = :schoolId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts
              LEFT JOIN ts.forCourse slotCourse LEFT JOIN ts.room slotRoom
              LEFT JOIN ts.timetable slotTimetable
              WHERE ts.teacher.id = :teacherId
              AND ts.forClass.academicYear.school.id = :schoolId
              AND (slotTimetable IS NULL OR slotTimetable.school.id = :schoolId)
              AND ts.period.school.id = :schoolId
              AND (slotCourse IS NULL OR slotCourse.school.id = :schoolId)
              AND (slotRoom IS NULL OR slotRoom.school.id = :schoolId)
            UNION
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
              AND ct.academicYear.school.id = :schoolId
        )
        ORDER BY c.name
        """)
    List<ClassEntity> findByTeacherIdAndSchoolId(@Param("teacherId") Long teacherId,
                                               @Param("schoolId") Long schoolId);
}
