package com.example.school_management.feature.academic.utils;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;
import java.util.function.Function;

import static com.example.school_management.feature.academic.utils.EntityIdExtractor.idOf;
import static com.example.school_management.feature.academic.dto.BatchIdsRequest.Operation.*;

public final class EnrollmentUtils {

    private EnrollmentUtils() {}

    /* ---------------- generic ADD / REMOVE helper ---------------- */
    public static <T> void applyBatch(Set<T> target,
                                      com.example.school_management.feature.academic.dto.BatchIdsRequest req,
                                      Function<Long,T> loader) {

        if (req.operation() == ADD) {
            req.ids().forEach(id -> target.add(loader.apply(id)));
        } else { // REMOVE
            target.removeIf(e -> req.ids().contains(idOf(e)));
        }
    }

    /* ---------------- repository fetch w/ 404 -------------------- */
    public static <T> T fetch(JpaRepository<T, Long> repo, Long id, String label) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(label + " not found"));
    }
}