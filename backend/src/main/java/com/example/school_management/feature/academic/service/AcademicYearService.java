package com.example.school_management.feature.academic.service;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.dto.AcademicYearDto;
import com.example.school_management.feature.academic.dto.CreateAcademicYearRequest;
import com.example.school_management.feature.academic.dto.UpdateAcademicYearRequest;
import com.example.school_management.feature.academic.entity.AcademicYear;
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
public class AcademicYearService {
    private final CurrentSchoolResolver currentSchool;
    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final Validator validator;

    @Transactional
    public AcademicYearDto create(CreateAcademicYearRequest request) {
        validate(request);
        var school = currentSchool.resolve();
        if (years.existsBySchoolIdAndName(school.getId(), request.name())) {
            throw new ConflictException("AcademicYear name already exists in current School");
        }
        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        years.saveAndFlush(year);
        if (request.active()) {
            year = activateYear(year.getId(), school.getId());
        }
        return AcademicYearDto.from(year);
    }

    public AcademicYearDto get(Long id) {
        return AcademicYearDto.from(findYear(id, currentSchool.resolve().getId()));
    }

    public List<AcademicYearDto> list() {
        return years.findBySchoolIdOrderByStartDateDescIdDesc(currentSchool.resolve().getId())
                .stream().map(AcademicYearDto::from).toList();
    }

    @Transactional
    public AcademicYearDto update(Long id, UpdateAcademicYearRequest request) {
        validate(request);
        Long schoolId = currentSchool.resolve().getId();
        AcademicYear year = years.findForUpdateByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear not found"));
        if (years.existsBySchoolIdAndNameAndIdNot(schoolId, request.name(), id)) {
            throw new ConflictException("AcademicYear name already exists in current School");
        }
        if (terms.existsOutsideDateRange(id, request.startDate(), request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AcademicYear dates must contain all existing Terms");
        }
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        return AcademicYearDto.from(years.saveAndFlush(year));
    }

    @Transactional
    public AcademicYearDto activate(Long id) {
        return AcademicYearDto.from(activateYear(id, currentSchool.resolve().getId()));
    }

    private AcademicYear findYear(Long id, Long schoolId) {
        return years.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear not found"));
    }

    private AcademicYear activateYear(Long id, Long schoolId) {
        // Lock before reading mutable year fields; activation must not overwrite concurrent calendar edits.
        var schoolYears = years.findAllForUpdateBySchoolId(schoolId);
        AcademicYear target = schoolYears.stream().filter(year -> year.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear not found"));
        if (target.isActive()) {
            return target;
        }
        schoolYears.stream().filter(AcademicYear::isActive).forEach(year -> year.setActive(false));
        // PostgreSQL's partial unique index is immediate: persist deactivation before activation.
        years.flush();
        target.setActive(true);
        return years.saveAndFlush(target);
    }

    private void validate(Object request) {
        if (!validator.validate(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AcademicYear fields or date range");
        }
    }
}
