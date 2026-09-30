package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.AcademicYear;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    List<AcademicYear> findBySchoolId(Long schoolId);

    Optional<AcademicYear> findBySchoolIdAndActiveTrue(Long schoolId);

    List<AcademicYear> findBySchoolIdOrderByStartDateDescIdDesc(Long schoolId);

    Optional<AcademicYear> findByIdAndSchoolId(Long id, Long schoolId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select y from AcademicYear y where y.id = :id and y.school.id = :schoolId")
    Optional<AcademicYear> findForUpdateByIdAndSchoolId(Long id, Long schoolId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select y from AcademicYear y where y.school.id = :schoolId order by y.id")
    List<AcademicYear> findAllForUpdateBySchoolId(Long schoolId);

    boolean existsBySchoolIdAndName(Long schoolId, String name);

    boolean existsBySchoolIdAndNameAndIdNot(Long schoolId, String name, Long id);

    List<AcademicYear> findAllBySchoolIdAndActiveTrue(Long schoolId);
}
