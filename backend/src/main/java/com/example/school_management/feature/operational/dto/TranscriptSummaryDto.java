package com.example.school_management.feature.operational.dto;

import lombok.Value;
import java.time.LocalDate;

@Value
public class TranscriptSummaryDto {
    Long studentId;
    String studentName;
    LocalDate enrollmentDate;
    LocalDate currentDate;
    Double overallGPA;
    Integer totalCredits;
    Integer totalCourses;
    String academicStanding;
    Double attendancePercentage;
    Integer totalAbsences;
    Integer totalExcusedAbsences;
    String gradeLevel;
    String currentClass;
} 