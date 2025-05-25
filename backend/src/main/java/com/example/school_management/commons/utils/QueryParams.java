package com.example.school_management.commons.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.*;

@Getter
@Setter
public class QueryParams {
    /** e.g. ["students","courses"] */
    private List<String> include = new ArrayList<>();
    /** e.g. {"class":["id","name","studentCount"]} */
    private Map<String,List<String>> fields = new HashMap<>();
    /** e.g. {"name":["math"], "levelId":["3"]} */
    private Map<String,List<String>> filters = new HashMap<>();
    /** e.g. [Sort.Order("name","ASC"), Sort.Order("createdAt","DESC")] */
    private List<Sort.Order> sort = new ArrayList<>();
    private int page = 0;
    private int size = 20;

    // getters & setters...

    @Override
    public String toString() {
        return "QueryParams{" +
                "include=" + include +
                ", fields=" + fields +
                ", filters=" + filters +
                ", sort=" + sort +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}
