package com.example.school_management.feature.academic.dto;

import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.annotation.JsonFilter;

import java.util.List;

@JsonFilter("fieldFilter")
@JsonResource("classView")
public record ClassViewDto(
        Long id,
        String name,
        List<AssignmentDto> courses) { }