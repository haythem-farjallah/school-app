package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    List<Term> findByAcademicYearIdOrderBySequenceNumberAsc(Long academicYearId);

    Optional<Term> findByIdAndAcademicYearId(Long id, Long academicYearId);

    boolean existsByAcademicYearIdAndName(Long academicYearId, String name);

    boolean existsByAcademicYearIdAndNameAndIdNot(Long academicYearId, String name, Long id);

    boolean existsByAcademicYearIdAndSequenceNumber(Long academicYearId, Integer sequenceNumber);

    boolean existsByAcademicYearIdAndSequenceNumberAndIdNot(Long academicYearId, Integer sequenceNumber, Long id);

    @Query("select (count(t) > 0) from Term t where t.academicYear.id = :yearId "
            + "and (t.startDate < :startDate or t.endDate > :endDate)")
    boolean existsOutsideDateRange(Long yearId, LocalDate startDate, LocalDate endDate);
}
