package com.example.school_management.feature.academic.dto;

import java.util.Set;

public record LevelDto(Long id, String name, Set<Long> courseIds) { }
