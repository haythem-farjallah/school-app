package com.example.school_management.feature.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentDto {
    private Long   id;
    private String firstName;
    private String lastName;
    private String email;
    private String gradeLevel;
    private Integer enrollmentYear;
}