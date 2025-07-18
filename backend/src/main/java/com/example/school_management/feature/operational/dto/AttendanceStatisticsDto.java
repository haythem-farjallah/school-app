package com.example.school_management.feature.operational.dto;

import lombok.Value;
import java.time.LocalDate;

@Value
public class AttendanceStatisticsDto {
    Long userId;
    String userName;
    String userType;
    LocalDate startDate;
    LocalDate endDate;
    Long totalDays;
    Long presentDays;
    Long absentDays;
    Long lateDays;
    Long excusedDays;
    Double attendancePercentage;
    Double absencePercentage;
} 