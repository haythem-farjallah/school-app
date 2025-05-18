package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.entity.BaseUser;
import org.mapstruct.*;

/**
 * Generic MapStruct contract:  *
 *   • C = “Create”  DTO  -> Entity         (toEntity)
 *   • U = “Update”  DTO  -> PATCH/merge    (patch)
 *
 * Concrete mappers (TeacherMapper, WorkerMapper, …) extend this interface
 * and supply the real types for <E, C, U>, plus any field-level @Mappings
 * they need.
 *
 * MapStruct **can** generate code for generic interfaces as of 1.5.x.
 */
@MapperConfig
public interface BaseUserMapper<
        E extends BaseUser,   // entity subtype
        C,                    // create DTO
        U,
        R> {                  // update DTO

    /** DTO (create) ➜ new Entity.  Id, password, permissions, etc. are
     usually ignored or set elsewhere in the service layer. */
    E toEntity(C dto);

    /** DTO (update) ➜ merge into an existing entity.  Only non-null
     properties from the DTO replace the entity fields. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patch(U dto, @MappingTarget E entity);

    R toDto(E entity);

}