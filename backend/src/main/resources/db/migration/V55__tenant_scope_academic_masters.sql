ALTER TABLE courses ADD COLUMN school_id BIGINT;
ALTER TABLE rooms ADD COLUMN school_id BIGINT;
ALTER TABLE periods ADD COLUMN school_id BIGINT;

DO $$
DECLARE
    school_count BIGINT;
    legacy_school_id BIGINT;
BEGIN
    SELECT count(*) INTO school_count FROM schools;

    IF school_count = 0 THEN
        INSERT INTO schools(name) VALUES ('Legacy School') RETURNING id INTO legacy_school_id;
    ELSIF school_count = 1 THEN
        SELECT id INTO legacy_school_id FROM schools;
    ELSIF EXISTS (SELECT 1 FROM courses WHERE school_id IS NULL)
       OR EXISTS (SELECT 1 FROM rooms WHERE school_id IS NULL)
       OR EXISTS (SELECT 1 FROM periods WHERE school_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot assign unscoped academic masters: multiple schools exist; explicit legacy ownership is required';
    END IF;

    IF legacy_school_id IS NOT NULL THEN
        UPDATE courses SET school_id = legacy_school_id WHERE school_id IS NULL;
        UPDATE rooms SET school_id = legacy_school_id WHERE school_id IS NULL;
        UPDATE periods SET school_id = legacy_school_id WHERE school_id IS NULL;
    END IF;
END $$;

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
    ALTER COLUMN school_id SET NOT NULL,
    DROP CONSTRAINT courses_code_key,
    ADD CONSTRAINT uk_courses_school_code UNIQUE (school_id, code);

ALTER TABLE rooms
    ADD CONSTRAINT fk_rooms_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
    ALTER COLUMN school_id SET NOT NULL;

CREATE INDEX idx_rooms_school ON rooms(school_id);

ALTER TABLE periods
    ADD CONSTRAINT fk_periods_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
    ALTER COLUMN school_id SET NOT NULL,
    DROP CONSTRAINT unique_period_index,
    ADD CONSTRAINT uk_periods_school_index UNIQUE (school_id, index_number);

-- Course and Period scoped unique indexes also cover their School foreign-key lookups.
