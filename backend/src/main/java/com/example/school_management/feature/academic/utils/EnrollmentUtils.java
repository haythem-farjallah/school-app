package com.example.school_management.feature.academic.utils;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public final class EnrollmentUtils {

    private EnrollmentUtils() {}

    /* ---------------- repository fetch w/ 404 -------------------- */
    public static <T> T fetch(JpaRepository<T, Long> repo, Long id, String label) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(label + " not found"));
    }
}