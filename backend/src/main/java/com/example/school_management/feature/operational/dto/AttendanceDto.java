package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceDto {
    // Read-only fields
    @JsonIgnore
    private Long id;
    @JsonIgnore
    private String userName;
    @JsonIgnore
    private String courseName;
    @JsonIgnore
    private String className;
    @JsonIgnore
    private String recordedByName;
    @JsonIgnore
    private LocalDateTime createdAt;
    @JsonIgnore
    private LocalDateTime updatedAt;
    
    // Write-only fields
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull 
    private Long userId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long courseId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long classId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long timetableSlotId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull 
    private LocalDate date;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull 
    private AttendanceStatus status;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull 
    private UserType userType;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String remarks;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String excuse;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String medicalNote;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long recordedById;

    // Constructor
    public AttendanceDto() {}

    public AttendanceDto(Long id, String userName, String courseName, String className, 
                        String recordedByName, LocalDateTime createdAt, LocalDateTime updatedAt,
                        Long userId, Long courseId, Long classId, Long timetableSlotId,
                        LocalDate date, AttendanceStatus status, UserType userType,
                        String remarks, String excuse, String medicalNote, Long recordedById) {
        this.id = id;
        this.userName = userName;
        this.courseName = courseName;
        this.className = className;
        this.recordedByName = recordedByName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.courseId = courseId;
        this.classId = classId;
        this.timetableSlotId = timetableSlotId;
        this.date = date;
        this.status = status;
        this.userType = userType;
        this.remarks = remarks;
        this.excuse = excuse;
        this.medicalNote = medicalNote;
        this.recordedById = recordedById;
    }

    // Getters
    public Long getId() { return id; }
    public String getUserName() { return userName; }
    public String getCourseName() { return courseName; }
    public String getClassName() { return className; }
    public String getRecordedByName() { return recordedByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public Long getClassId() { return classId; }
    public Long getTimetableSlotId() { return timetableSlotId; }
    public LocalDate getDate() { return date; }
    public AttendanceStatus getStatus() { return status; }
    public UserType getUserType() { return userType; }
    public String getRemarks() { return remarks; }
    public String getExcuse() { return excuse; }
    public String getMedicalNote() { return medicalNote; }
    public Long getRecordedById() { return recordedById; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setClassName(String className) { this.className = className; }
    public void setRecordedByName(String recordedByName) { this.recordedByName = recordedByName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public void setTimetableSlotId(Long timetableSlotId) { this.timetableSlotId = timetableSlotId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setExcuse(String excuse) { this.excuse = excuse; }
    public void setMedicalNote(String medicalNote) { this.medicalNote = medicalNote; }
    public void setRecordedById(Long recordedById) { this.recordedById = recordedById; }
} 