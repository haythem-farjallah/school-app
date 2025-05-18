package com.example.school_management.feature.academic.dto;

import java.util.Set;

public record ClassDto(Long id,
                       String name,
                       Long levelId,
                       Set<Long> studentIds,
                       Set<Long> courseIds,
                       Long scheduleId) { }