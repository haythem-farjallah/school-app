package com.example.school_management.feature.operational.dto;

import lombok.Value;
import java.time.LocalDate;

@Value
public class TranscriptCourseDto {
    String courseName;
    String courseCode;
    Double grade;
    Double credits;
    String gradeLetter;
    String teacherName;
    LocalDate gradedDate;
} 