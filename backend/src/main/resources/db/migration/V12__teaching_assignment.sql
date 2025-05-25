-- 1) table ------------------------------------------------------------
CREATE TABLE teaching_assignments (
                                      id          BIGSERIAL PRIMARY KEY,
                                      class_id    BIGINT NOT NULL,
                                      course_id   BIGINT NOT NULL,
                                      teacher_id  BIGINT NOT NULL,
                                      weekly_hours INT DEFAULT 0,

                                      CONSTRAINT fk_ta_class   FOREIGN KEY (class_id)   REFERENCES classes(id),
                                      CONSTRAINT fk_ta_course  FOREIGN KEY (course_id)  REFERENCES courses(id),
                                      CONSTRAINT fk_ta_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id),
                                      CONSTRAINT uc_class_course UNIQUE (class_id, course_id)
);

-- 2) helpful index for dashboards ------------------------------------
CREATE INDEX idx_ta_class      ON teaching_assignments(class_id);
CREATE INDEX idx_ta_teacher    ON teaching_assignments(teacher_id);
