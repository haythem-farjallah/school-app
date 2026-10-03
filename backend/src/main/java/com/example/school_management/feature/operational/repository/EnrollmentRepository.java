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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.id = :classId ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByClassId(@Param("classId") Long classId, Pageable pageable);

    // A Student may have many historical Enrollments in one Class, but at most one ACTIVE one:
    // the database allows a single ACTIVE Enrollment per Student per AcademicYear.
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.classEntity.id = :classId AND e.status = :status")
    Optional<Enrollment> findByStudentIdAndClassIdAndStatus(@Param("studentId") Long studentId, @Param("classId") Long classId,
                                                            @Param("status") EnrollmentStatus status);

    @Query("SELECT COUNT(e) > 0 FROM Enrollment e WHERE e.student.id = :studentId "
            + "AND e.classEntity.academicYear.id = :academicYearId AND e.status = :status")
    boolean existsByStudentIdAndAcademicYearIdAndStatus(@Param("studentId") Long studentId,
                                                        @Param("academicYearId") Long academicYearId,
                                                        @Param("status") EnrollmentStatus status);

    @Query("SELECT e.student.id FROM Enrollment e WHERE e.classEntity.academicYear.id = :academicYearId "
            + "AND e.status = :status")
    List<Long> findStudentIdsByAcademicYearIdAndStatus(@Param("academicYearId") Long academicYearId,
                                                     @Param("status") EnrollmentStatus status);

    default Optional<Enrollment> findActiveByStudentIdAndClassId(Long studentId, Long classId) {
        return findByStudentIdAndClassIdAndStatus(studentId, classId, EnrollmentStatus.ACTIVE);
    }

    default boolean existsActiveInClass(Long studentId, Long classId) {
        return findActiveByStudentIdAndClassId(studentId, classId).isPresent();
    }

    default boolean existsActiveInAcademicYear(Long studentId, Long academicYearId) {
        return existsByStudentIdAndAcademicYearIdAndStatus(studentId, academicYearId, EnrollmentStatus.ACTIVE);
    }

    default long countActiveByClassId(Long classId) {
        return countByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
    }

    // ---- The current roster of a Class is its ACTIVE Enrollments; every roster read goes through these.

    interface RosterRow {
        Long getClassId();
        Long getStudentId();
    }

    interface RosterCountRow {
        Long getClassId();
        Long getStudentCount();
    }

    @Query("SELECT e.student.id FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = :status")
    List<Long> findStudentIdsByClassIdAndStatus(@Param("classId") Long classId, @Param("status") EnrollmentStatus status);

    @Query("SELECT e.classEntity.id AS classId, e.student.id AS studentId FROM Enrollment e "
            + "WHERE e.classEntity.id IN :classIds AND e.status = :status")
    List<RosterRow> findRosterRowsByClassIdsAndStatus(@Param("classIds") Collection<Long> classIds,
                                                      @Param("status") EnrollmentStatus status);

    @Query("SELECT e.classEntity.id AS classId, COUNT(e) AS studentCount FROM Enrollment e "
            + "WHERE e.classEntity.id IN :classIds AND e.status = :status GROUP BY e.classEntity.id")
    List<RosterCountRow> countRosterByClassIdsAndStatus(@Param("classIds") Collection<Long> classIds,
                                                        @Param("status") EnrollmentStatus status);

    default List<Long> findActiveStudentIdsByClassId(Long classId) {
        return findStudentIdsByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
    }

    default List<RosterRow> findActiveRosterRows(Collection<Long> classIds) {
        return classIds.isEmpty() ? List.of() : findRosterRowsByClassIdsAndStatus(classIds, EnrollmentStatus.ACTIVE);
    }

    default List<RosterCountRow> countActiveRosters(Collection<Long> classIds) {
        return classIds.isEmpty() ? List.of() : countRosterByClassIdsAndStatus(classIds, EnrollmentStatus.ACTIVE);
    }

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = :status ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByClassIdAndStatus(@Param("classId") Long classId, @Param("status") EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = :status")
    Long countByClassIdAndStatus(@Param("classId") Long classId, @Param("status") EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status")
    Long countActiveEnrollmentsByStudentId(@Param("studentId") Long studentId, @Param("status") EnrollmentStatus status);
    
    // Count all enrollments for a student (regardless of status)
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = :studentId")
    Long countByStudentId(@Param("studentId") Long studentId);
    
    // Find enrollments by student ID (without pagination)
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByStudentId(@Param("studentId") Long studentId);
    
    // ---- Enrollment administration is limited to one School: Enrollment -> Class -> AcademicYear -> School.
    // The School constraint is part of every query, so paging and totals never include foreign rows.

    @Query("SELECT e FROM Enrollment e WHERE e.id = :id AND e.classEntity.academicYear.school.id = :schoolId")
    Optional<Enrollment> findByIdAndSchoolId(@Param("id") Long id, @Param("schoolId") Long schoolId);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.academicYear.school.id = :schoolId")
    Page<Enrollment> findBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.academicYear.school.id = :schoolId "
            + "AND e.student.id = :studentId ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByStudentIdAndSchoolId(@Param("studentId") Long studentId, @Param("schoolId") Long schoolId,
                                                Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.academicYear.school.id = :schoolId "
            + "AND e.student.id = :studentId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findAllByStudentIdAndSchoolId(@Param("studentId") Long studentId, @Param("schoolId") Long schoolId);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.academicYear.school.id = :schoolId "
            + "AND e.status = :status ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByStatusAndSchoolId(@Param("status") EnrollmentStatus status, @Param("schoolId") Long schoolId,
                                             Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.academicYear.school.id = :schoolId "
            + "AND e.enrolledAt BETWEEN :startDate AND :endDate ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findByEnrolledAtBetweenAndSchoolId(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate,
                                                        @Param("schoolId") Long schoolId, Pageable pageable);

    // Search enrollments by student name or class name
    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.academicYear.school.id = :schoolId AND (" +
           "LOWER(e.student.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.student.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.classEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ") ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findBySearchAndSchoolId(@Param("search") String search, @Param("schoolId") Long schoolId,
                                             Pageable pageable);

    // Search enrollments by student name or class name and status
    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.academicYear.school.id = :schoolId AND " +
           "e.status = :status AND (" +
           "LOWER(e.student.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.student.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.classEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ") ORDER BY e.enrolledAt DESC")
    Page<Enrollment> findBySearchAndStatusAndSchoolId(@Param("search") String search, @Param("status") EnrollmentStatus status,
                                                      @Param("schoolId") Long schoolId, Pageable pageable);

    // Get all enrollments for a class (regardless of status)
    @Query("SELECT e FROM Enrollment e WHERE e.classEntity.id = :classId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findAllByClassId(@Param("classId") Long classId);
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.status = com.example.school_management.feature.operational.entity.enums.EnrollmentStatus.ACTIVE "
            + "AND e.classEntity.academicYear.id = :academicYearId AND e.classEntity.academicYear.school.id = :schoolId")
    Optional<Enrollment> findActiveByStudentIdAndAcademicYearIdAndSchoolId(@Param("studentId") Long studentId,
            @Param("academicYearId") Long academicYearId, @Param("schoolId") Long schoolId);
} 