package com.example.school_management.commons.utils;


import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

public final class EntitySpecification {

    private EntitySpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> Specification<T> hasAttribute(String attribute, String value) {

        return (root, query, criteriaBuilder) -> {
            if (attribute != null && value != null) {
                Path<?> path = root.get(attribute);

                // Vérifier le type de l'attribut et appliquer la logique de recherche appropriée
                if (path.getJavaType().equals(String.class)) {
                    return criteriaBuilder.like(path.as(String.class), "%" + value + "%");
                } else if (path.getJavaType().equals(Boolean.class)) {
                    Boolean booleanValue = Boolean.valueOf(value);
                    return criteriaBuilder.equal(path.as(Boolean.class), booleanValue);
                } else if (path.getJavaType().equals(Integer.class)) {
                    Integer intValue = Integer.valueOf(value);
                    return criteriaBuilder.equal(path.as(Integer.class), intValue);
                }
                // Ajouter d'autres types si nécessaire (par exemple, Long, Date, etc.)

            }
            return null;
        };
    }


}