package com.example.school_management.feature.operational.dto;

import lombok.Value;
import java.time.LocalDate;

@Value
public class EmploymentCertificateDto {
    Long employeeId;
    String employeeName;
    String employeeEmail;
    String employeePosition;
    String department;
    LocalDate hireDate;
    LocalDate certificateDate;
    String employmentStatus; // ACTIVE, SUSPENDED, TERMINATED
    String workSchedule; // Full-time, Part-time, etc.
    String salaryRange; // Optional - for internal use
    String supervisorName;
    String supervisorPosition;
    String schoolName;
    String schoolAddress;
    String schoolPhone;
    String schoolEmail;
    String certificateNumber;
    String additionalNotes;
} 