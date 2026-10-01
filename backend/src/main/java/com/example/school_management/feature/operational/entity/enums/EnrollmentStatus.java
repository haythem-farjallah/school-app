package com.example.school_management.feature.operational.entity.enums;

public enum EnrollmentStatus {
    ACTIVE, COMPLETED, TRANSFERRED, WITHDRAWN;

    /** A terminal Enrollment is history: it never changes status again. */
    public boolean isTerminal() {
        return this != ACTIVE;
    }
}
