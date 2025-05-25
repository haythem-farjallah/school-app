package com.example.school_management.commons.json;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonResource {
    /**
     * The singular resource name, e.g. "level", "class", "course"
     * to match the fields[<name>]=... query-param key.
     */
    String value();
}