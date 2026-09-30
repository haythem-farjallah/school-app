package com.example.school_management.feature.academic.service;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.dto.TermDto;
import com.example.school_management.feature.academic.dto.TermRequest;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.Term;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.TermRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermService {
    private final CurrentSchoolResolver currentSchool;
    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final Validator validator;

    @Transactional
    public TermDto create(Long academicYearId, TermRequest request) {
        AcademicYear year = findYear(academicYearId, true);
        validate(year, request);
        if (terms.existsByAcademicYearIdAndName(academicYearId, request.name())) {
            throw new ConflictException("Term name already exists in AcademicYear");
        }
        if (terms.existsByAcademicYearIdAndSequenceNumber(academicYearId, request.sequenceNumber())) {
            throw new ConflictException("Term sequence already exists in AcademicYear");
        }
        Term term = new Term();
        term.setAcademicYear(year);
        setFields(term, request);
        return TermDto.from(terms.saveAndFlush(term));
    }

    public TermDto get(Long academicYearId, Long termId) {
        findYear(academicYearId, false);
        return TermDto.from(findTerm(academicYearId, termId));
    }

    public List<TermDto> list(Long academicYearId) {
        findYear(academicYearId, false);
        return terms.findByAcademicYearIdOrderBySequenceNumberAsc(academicYearId)
                .stream().map(TermDto::from).toList();
    }

    @Transactional
    public TermDto update(Long academicYearId, Long termId, TermRequest request) {
        AcademicYear year = findYear(academicYearId, true);
        Term term = findTerm(academicYearId, termId);
        validate(year, request);
        if (terms.existsByAcademicYearIdAndNameAndIdNot(academicYearId, request.name(), termId)) {
            throw new ConflictException("Term name already exists in AcademicYear");
        }
        if (terms.existsByAcademicYearIdAndSequenceNumberAndIdNot(academicYearId, request.sequenceNumber(), termId)) {
            throw new ConflictException("Term sequence already exists in AcademicYear");
        }
        setFields(term, request);
        return TermDto.from(terms.saveAndFlush(term));
    }

    private AcademicYear findYear(Long id, boolean forUpdate) {
        Long schoolId = currentSchool.resolve().getId();
        // Share the year-row lock with calendar updates to preserve containment during concurrent edits.
        return (forUpdate ? years.findForUpdateByIdAndSchoolId(id, schoolId) : years.findByIdAndSchoolId(id, schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear not found"));
    }

    private Term findTerm(Long yearId, Long termId) {
        return terms.findByIdAndAcademicYearId(termId, yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
    }

    private void validate(AcademicYear year, TermRequest request) {
        if (!validator.validate(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Term fields or date range");
        }
        if (request.startDate().isBefore(year.getStartDate()) || request.endDate().isAfter(year.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Term dates must fit inside AcademicYear");
        }
    }

    private void setFields(Term term, TermRequest request) {
        term.setName(request.name());
        term.setSequenceNumber(request.sequenceNumber());
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
    }
}
