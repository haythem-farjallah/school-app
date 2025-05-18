package com.example.school_management.feature.unit.academic.mapper;

import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.auth.entity.Student;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicMapperTest {

    private final AcademicMapper mapper = Mappers.getMapper(AcademicMapper.class);

    /* ───────────────────── entity ➜ dto  ───────────────────── */

    @Test
    void classEntity_isMapped_to_ClassDto() {
        // given
        Student st = new Student();       st.setId(99L);
        Course  co = new Course();        co.setId(77L);

        Level level = new Level();        level.setId(3L);

        ClassEntity cl = new ClassEntity();
        cl.setId(1L);
        cl.setName("3-A");
        cl.setLevel(level);
        cl.setStudents(Set.of(st));
        cl.setCourses(Set.of(co));

        // when
        ClassDto dto = mapper.toClassDto(cl);

        // then
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.levelId()).isEqualTo(3L);
        assertThat(dto.studentIds()).containsExactly(99L);
        assertThat(dto.courseIds()).containsExactly(77L);
    }

    @Test
    void levelEntity_isMapped_to_LevelDto_with_course_and_class_ids() {
        Course c1 = new Course(); c1.setId(11L);
        Course c2 = new Course(); c2.setId(12L);

        ClassEntity cls1 = new ClassEntity(); cls1.setId(21L);
        ClassEntity cls2 = new ClassEntity(); cls2.setId(22L);

        Level level = new Level();
        level.setId(5L);
        level.setName("Level 3");
        level.setCourses(Set.of(c1, c2));
        level.setClasses(Set.of(cls1, cls2));

        LevelDto dto = mapper.toLevelDto(level);

        assertThat(dto.courseIds()).containsExactlyInAnyOrder(11L, 12L);
    }

    /* ───────────────────── patch / update ───────────────────── */

    @Test
    void updateCourseEntity_overwrites_nonNull_fields_only() {
        Course entity = new Course();
        entity.setName("Math");
        entity.setColor("#111");
        entity.setCoefficient(2.0);

        UpdateCourseRequest patch = new UpdateCourseRequest(
                "Advanced Math",   // change
                null,              // keep old color
                3.0,               // change
                null               // teacher unchanged
        );

        mapper.updateCourseEntity(patch, entity);

        assertThat(entity.getName()).isEqualTo("Advanced Math");
        assertThat(entity.getColor()).isEqualTo("#111");          // untouched
        assertThat(entity.getCoefficient()).isEqualTo(3.0);
    }

    @Test
    void updateClassEntity_respects_nullValue_ignore_strategy() {
        ClassEntity entity = new ClassEntity();
        entity.setName("3-B");

        UpdateClassRequest patch = new UpdateClassRequest(null, null); // nothing to change
        mapper.updateClassEntity(patch, entity);

        assertThat(entity.getName()).isEqualTo("3-B"); // unchanged
    }
}
