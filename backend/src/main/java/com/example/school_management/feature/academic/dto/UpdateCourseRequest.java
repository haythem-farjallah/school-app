package com.example.school_management.feature.academic.dto;

public record UpdateCourseRequest(String name,
                                  String color,
                                  Double coefficient,
                                  Long teacherId) { }
