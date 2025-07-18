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


        ClassEntity cl = new ClassEntity();
        cl.setId(1L);
        cl.setName("3-A");
        cl.setStudents(Set.of(st));
        cl.setCourses(Set.of(co));

        // when
        ClassDto dto = mapper.toClassDto(cl);

        // then
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.studentIds()).containsExactly(99L);
        assertThat(dto.courseIds()).containsExactly(77L);
    }


    /* ───────────────────── patch / update ───────────────────── */

    @Test
    void updateCourseEntity_overwrites_nonNull_fields_only() {
        Course entity = new Course();
        entity.setName("Math");
        entity.setColor("#111");
        entity.setCredit(2.0f);
        entity.setWeeklyCapacity(3);

        UpdateCourseRequest patch = new UpdateCourseRequest(
                "Advanced Math",   // change
                null,              // keep old color
                3.0f,              // change
                4,                 // change weeklyCapacity
                null               // teacher unchanged
        );

        mapper.updateCourseEntity(patch, entity);

        assertThat(entity.getName()).isEqualTo("Advanced Math");
        assertThat(entity.getColor()).isEqualTo("#111");          // untouched
        assertThat(entity.getCredit()).isEqualTo(3.0f);
        assertThat(entity.getWeeklyCapacity()).isEqualTo(4);
    }

    @Test
    void updateClassEntity_respects_nullValue_ignore_strategy() {
        ClassEntity entity = new ClassEntity();
        entity.setName("3-B");

        UpdateClassRequest patch = new UpdateClassRequest(null); // nothing to change
        mapper.updateClassEntity(patch, entity);

        assertThat(entity.getName()).isEqualTo("3-B"); // unchanged
    }
}
