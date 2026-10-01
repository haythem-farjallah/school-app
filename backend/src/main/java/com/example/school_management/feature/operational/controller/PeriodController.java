package com.example.school_management.feature.operational.controller;

import com.example.school_management.feature.operational.dto.PeriodDto;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/periods")
@RequiredArgsConstructor
public class PeriodController {

    private final PeriodRepository periodRepository;
    private final OperationalMapper mapper;
    private final CurrentSchoolResolver currentSchool;

    @GetMapping
    public List<PeriodDto> getAllPeriods() {
        List<Period> periods = periodRepository.findBySchoolIdOrderByIndex(currentSchool.resolve().getId());
        return periods.stream()
                .map(mapper::toPeriodDto)
                .collect(Collectors.toList());
    }
}
