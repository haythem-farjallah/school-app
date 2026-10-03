ALTER TABLE timetables ADD COLUMN school_id BIGINT;

-- Structural references own legacy Timetables. Free-text labels and Teachers do not.
CREATE TEMP TABLE timetable_school_ownership ON COMMIT DROP AS
WITH evidence AS (
    SELECT tc.timetable_id, ay.school_id
    FROM timetable_classes tc
    JOIN classes c ON c.id = tc.class_id
    JOIN academic_years ay ON ay.id = c.academic_year_id
    UNION ALL
    SELECT tr.timetable_id, r.school_id
    FROM timetable_rooms tr
    JOIN rooms r ON r.id = tr.room_id
    UNION ALL
    SELECT slot.timetable_id, p.school_id
    FROM timetable_slots slot
    JOIN periods p ON p.id = slot.period_id
    WHERE slot.timetable_id IS NOT NULL
), only_school AS (
    SELECT count(*) AS school_count, min(id) AS school_id FROM schools
)
SELECT t.id AS timetable_id,
       count(DISTINCT e.school_id) AS school_count,
       CASE WHEN count(e.timetable_id) > 0 THEN min(e.school_id)
            WHEN only_school.school_count = 1 THEN only_school.school_id
       END AS school_id,
       bool_or(e.timetable_id IS NOT NULL AND e.school_id IS NULL) AS missing_context_school
FROM timetables t
CROSS JOIN only_school
LEFT JOIN evidence e ON e.timetable_id = t.id
GROUP BY t.id, only_school.school_count, only_school.school_id;

-- Check slot context first, including standalone Slots. Never repair conflicting links.
DO $$
DECLARE
    examples TEXT;
BEGIN
    SELECT string_agg('slot ' || id, ', ' ORDER BY id) INTO examples
    FROM (
        SELECT slot.id
        FROM timetable_slots slot
        JOIN periods p ON p.id = slot.period_id
        LEFT JOIN classes c ON c.id = slot.for_class_id
        LEFT JOIN academic_years ay ON ay.id = c.academic_year_id
        LEFT JOIN courses course ON course.id = slot.for_course_id
        LEFT JOIN rooms r ON r.id = slot.room_id
        WHERE (slot.for_class_id IS NOT NULL AND ay.school_id IS DISTINCT FROM p.school_id)
           OR (slot.for_course_id IS NOT NULL AND course.school_id IS DISTINCT FROM p.school_id)
           OR (slot.room_id IS NOT NULL AND r.school_id IS DISTINCT FROM p.school_id)
           OR (slot.teacher_id IS NOT NULL
               AND EXISTS (SELECT 1 FROM school_memberships m WHERE m.user_id = slot.teacher_id)
               AND NOT EXISTS (
                   SELECT 1 FROM school_memberships m
                   JOIN school_membership_roles role ON role.membership_id = m.id
                   WHERE m.user_id = slot.teacher_id AND m.school_id = p.school_id AND role.role = 'TEACHER'
               ))
        ORDER BY slot.id LIMIT 10
    ) invalid;

    IF examples IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot determine Timetable School ownership: % has context incompatible with its Period School. Reconcile legacy ownership before migrating.', examples;
    END IF;

    SELECT string_agg('timetable ' || timetable_id, ', ' ORDER BY timetable_id) INTO examples
    FROM (
        SELECT timetable_id FROM timetable_school_ownership
        WHERE school_count > 1 OR school_id IS NULL OR missing_context_school
        ORDER BY timetable_id LIMIT 10
    ) invalid;

    IF examples IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot determine Timetable School ownership for %. Structural Schools must agree; Timetables without structural evidence require exactly one School. Reconcile legacy ownership before migrating.', examples;
    END IF;

    -- Existing membership information must support the structural owner. Leave Teachers
    -- without membership history unchanged; this migration must not invent memberships.
    SELECT string_agg('timetable ' || timetable_id, ', ' ORDER BY timetable_id) INTO examples
    FROM (
        SELECT DISTINCT ownership.timetable_id
        FROM timetable_school_ownership ownership
        JOIN timetable_teachers tt ON tt.timetable_id = ownership.timetable_id
        WHERE EXISTS (SELECT 1 FROM school_memberships m WHERE m.user_id = tt.teacher_id)
          AND NOT EXISTS (
              SELECT 1 FROM school_memberships m
              JOIN school_membership_roles role ON role.membership_id = m.id
              WHERE m.user_id = tt.teacher_id AND m.school_id = ownership.school_id AND role.role = 'TEACHER'
          )
        ORDER BY ownership.timetable_id LIMIT 10
    ) invalid;

    IF examples IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot determine Timetable School ownership for %: linked Teacher memberships are incompatible with the owner School. Reconcile legacy ownership before migrating.', examples;
    END IF;
END $$;

UPDATE timetables t
SET school_id = ownership.school_id
FROM timetable_school_ownership ownership
WHERE ownership.timetable_id = t.id;

ALTER TABLE timetables
    ALTER COLUMN school_id SET NOT NULL,
    ADD CONSTRAINT fk_timetables_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT;

-- The leading School key supports unfiltered lists as well as label-filtered reads.
CREATE INDEX idx_timetables_school_academic_year_semester ON timetables(school_id, academic_year, semester);
