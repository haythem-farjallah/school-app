package com.example.school_management.feature.operational.dto;

import lombok.Value;
import java.time.LocalDate;
import java.util.List;

@Value
public class TranscriptDto {
    Long studentId;
    String studentName;
    String studentEmail;
    LocalDate enrollmentDate;
    LocalDate transcriptDate;
    Double overallGPA;
    Integer totalCredits;
    String academicStanding;
    List<TranscriptCourseDto> allCourses;
    TranscriptSummaryDto summary;
} 