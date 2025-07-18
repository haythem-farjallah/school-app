package com.example.school_management.feature.academic.utils;

import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.auth.entity.Student;

public final class EntityIdExtractor {

    private EntityIdExtractor() {}

    public static Long idOf(Object entity) {
        if (entity instanceof Student s)      return s.getId();
        if (entity instanceof Course c)       return c.getId();
        if (entity instanceof ClassEntity ce) return ce.getId();
        throw new IllegalArgumentException("Unsupported entity: " + entity.getClass());
    }
}
