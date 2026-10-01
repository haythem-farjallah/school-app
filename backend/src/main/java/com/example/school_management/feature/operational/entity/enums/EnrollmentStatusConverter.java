package com.example.school_management.feature.operational.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores EnrollmentStatus by name. The database CHECK constraint guarantees valid values, so an
 * unknown value is a schema problem and fails loudly instead of being mapped to another status.
 */
@Converter(autoApply = true)
public class EnrollmentStatusConverter implements AttributeConverter<EnrollmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(EnrollmentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EnrollmentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EnrollmentStatus.valueOf(dbData);
    }
}
