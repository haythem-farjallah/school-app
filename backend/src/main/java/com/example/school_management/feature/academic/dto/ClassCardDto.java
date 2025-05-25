package com.example.school_management.feature.academic.dto;

import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("fieldFilter")
@JsonResource("classCard")
public record ClassCardDto(
        Long   id,
        String name,
        String levelName,
        int    studentCount,
        int    courseCount,
        int    teacherCount) { }