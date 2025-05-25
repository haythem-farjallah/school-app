

CREATE TABLE level_courses (
                               level_id BIGINT NOT NULL,
                               course_id BIGINT NOT NULL,
                               PRIMARY KEY (level_id, course_id),
                               CONSTRAINT fk_level FOREIGN KEY (level_id) REFERENCES levels(id) ON DELETE CASCADE,
                               CONSTRAINT fk_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);
