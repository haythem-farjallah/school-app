package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermRepository extends JpaRepository<Term, Long> {
    List<Term> findByAcademicYearIdOrderBySequenceNumberAsc(Long academicYearId);
}
