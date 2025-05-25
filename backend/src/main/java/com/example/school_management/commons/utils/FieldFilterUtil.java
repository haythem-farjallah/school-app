package com.example.school_management.commons.utils;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.http.converter.json.MappingJacksonValue;

import java.util.*;

public final class FieldFilterUtil {
    private FieldFilterUtil() {}

    public static MappingJacksonValue apply(MappingJacksonValue wrapper,
                                            QueryParams qp) {

        // 1) Find the “inner” DTO instance:
        Object value = wrapper.getValue();
        Object dtoInstance = extractDtoInstance(value);
        if (dtoInstance == null) {
            // nothing to filter
            return wrapper;
        }

        // 2) Read your @JsonResource annotation
        Class<?> dtoClass = dtoInstance.getClass();
        JsonResource ann = dtoClass.getAnnotation(JsonResource.class);
        if (ann == null) {
            // no annotation → no sparse-field filtering
            return wrapper;
        }
        String resourceName = ann.value();  // e.g. "level", "class", "course"

        // 3) Look up the requested fields for that resource
        List<String> props = qp.getFields()
                .getOrDefault(resourceName, Collections.emptyList());

        // 4) Build a filter provider under the id "fieldFilter"
        SimpleFilterProvider provider = new SimpleFilterProvider();
        if (props.isEmpty()) {
            // no sparse-field request → serialize everything
            provider.addFilter(
                    "fieldFilter",
                    SimpleBeanPropertyFilter.serializeAll()
            );
        } else {
            // only include exactly the requested props
            provider.addFilter(
                    "fieldFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept(new HashSet<>(props))
            );
        }

        // 5) Attach it and return
        wrapper.setFilters(provider);
        return wrapper;
    }


    /**
     * Given a wrapped value, try to find one DTO instance:
     * - ApiSuccessResponse<PageDto<DTO>> → .data().content().get(0)
     * - ApiSuccessResponse<DTO>           → .data()
     * - PageDto<DTO>                      → .content().get(0)
     * - DTO                               → itself
     */
    private static Object extractDtoInstance(Object value) {
        if (value instanceof ApiSuccessResponse<?> resp) {
            Object data = resp.getData();
            if (data instanceof PageDto<?> page && !page.content().isEmpty()) {
                return page.content().get(0);
            }
            return data;
        }
        if (value instanceof PageDto<?> page && !page.content().isEmpty()) {
            return page.content().get(0);
        }
        return value;
    }
}
