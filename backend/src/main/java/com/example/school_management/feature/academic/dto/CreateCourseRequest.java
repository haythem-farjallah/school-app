package com.example.school_management.feature.academic.dto;

public record CreateCourseRequest(String name,
                                  String color,
                                  Float credit,
                                  Integer weeklyCapacity,
                                  Long teacherId) { }