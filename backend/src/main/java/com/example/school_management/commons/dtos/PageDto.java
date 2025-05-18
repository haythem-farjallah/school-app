package com.example.school_management.commons.dtos;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageDto<T>(List<T> content,
                         int page,
                         int size,
                         long totalElements) {
    public PageDto(Page<T> p) {
        this(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements());
    }
}