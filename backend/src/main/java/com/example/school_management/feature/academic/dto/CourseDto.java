package com.example.school_management.feature.academic.dto;

import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("fieldFilter")
@JsonResource("course")
public record CourseDto(Long id, String name, String color, Double coefficient, Long teacherId) { }
