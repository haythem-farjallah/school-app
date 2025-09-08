package com.example.school_management.feature.operational.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for EnrollmentStatus enum to handle PostgreSQL enum type conversion
 */
@Converter(autoApply = true)
public class EnrollmentStatusConverter implements AttributeConverter<EnrollmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(EnrollmentStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public EnrollmentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return EnrollmentStatus.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log the error and return a default value
            System.err.println("Invalid enrollment status value in database: " + dbData + ". Using PENDING as default.");
            return EnrollmentStatus.PENDING;
        }
    }
}
