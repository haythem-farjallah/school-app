package com.example.school_management.feature.academic.dto;

public record CreateCourseRequest(String name,
                                  String color,
                                  Double coefficient,
                                  Long teacherId) { }