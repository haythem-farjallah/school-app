package com.example.school_management.feature.school.service;

import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the legacy application's single School until explicit tenant selection exists. */
@Service
@RequiredArgsConstructor
public class CurrentSchoolResolver {
    private final SchoolRepository schools;

    @Transactional(readOnly = true)
    public School resolve() {
        var candidates = schools.findAll(PageRequest.of(0, 2)).getContent();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Single-school compatibility mode cannot operate: no Schools exist");
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Single-school compatibility mode is ambiguous: multiple Schools exist");
        }
        return candidates.get(0);
    }
}
