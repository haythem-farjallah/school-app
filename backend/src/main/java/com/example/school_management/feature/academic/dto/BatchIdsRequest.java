package com.example.school_management.feature.academic.dto;

import java.util.Set;

public record BatchIdsRequest(
        Operation operation,
        Set<Long> ids) {

    public enum Operation { ADD, REMOVE }
}