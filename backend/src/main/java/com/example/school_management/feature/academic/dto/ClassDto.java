package com.example.school_management.feature.academic.dto;

import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.annotation.JsonFilter;

import java.util.Set;

@JsonFilter("fieldFilter")
@JsonResource("class")
public record ClassDto(Long id,
                       String name,
                       Long levelId,
                       Set<Long> studentIds,
                       Set<Long> courseIds,
                       Long scheduleId) { }