package com.example.school_management.feature.academic.dto;

import com.example.school_management.feature.academic.entity.Term;

import java.time.LocalDate;

public record TermDto(Long id, Long academicYearId, String name, Integer sequenceNumber,
                      LocalDate startDate, LocalDate endDate) {
    public static TermDto from(Term term) {
        return new TermDto(term.getId(), term.getAcademicYear().getId(), term.getName(),
                term.getSequenceNumber(), term.getStartDate(), term.getEndDate());
    }
}
