ALTER TABLE teacher_attendance ADD COLUMN school_id BIGINT;

-- Class and Course are structural owners. Teacher memberships are evidence only
-- when both references are absent; membership status never erases attendance history.
CREATE TEMP TABLE teacher_attendance_school_ownership ON COMMIT DROP AS
WITH only_school AS (
    SELECT count(*) AS school_count, min(id) AS school_id FROM schools
)
SELECT a.id AS attendance_id,
       CASE
           WHEN a.class_id IS NOT NULL THEN ay.school_id
           WHEN a.course_id IS NOT NULL THEN course.school_id
           WHEN memberships.teacher_school_count = 1 THEN memberships.school_id
           WHEN NOT memberships.has_history AND only_school.school_count = 1 THEN only_school.school_id
       END AS school_id,
       (teacher.id IS NULL
        OR (a.class_id IS NOT NULL AND (c.id IS NULL OR ay.school_id IS NULL))
        OR (a.course_id IS NOT NULL AND (course.id IS NULL OR course.school_id IS NULL))
        OR (a.substitute_teacher_id IS NOT NULL AND substitute.id IS NULL)) AS missing_resource,
       (a.class_id IS NOT NULL AND a.course_id IS NOT NULL
        AND ay.school_id IS DISTINCT FROM course.school_id) AS conflicting_context
FROM teacher_attendance a
LEFT JOIN teacher ON teacher.id = a.teacher_id
LEFT JOIN teacher substitute ON substitute.id = a.substitute_teacher_id
LEFT JOIN classes c ON c.id = a.class_id
LEFT JOIN academic_years ay ON ay.id = c.academic_year_id
LEFT JOIN courses course ON course.id = a.course_id
CROSS JOIN only_school
CROSS JOIN LATERAL (
    SELECT EXISTS (SELECT 1 FROM school_memberships m WHERE m.user_id = a.teacher_id) AS has_history,
           count(DISTINCT m.school_id) AS teacher_school_count, min(m.school_id) AS school_id
    FROM school_memberships m
    JOIN school_membership_roles role ON role.membership_id = m.id AND role.role = 'TEACHER'
    WHERE m.user_id = a.teacher_id
) memberships;

-- Validate all evidence before backfilling. Flyway rolls back the entire migration
-- on failure, including the added column; never retain unverifiable scalar IDs.
DO $$
DECLARE
    examples TEXT;
BEGIN
    SELECT string_agg('attendance ' || attendance_id, ', ' ORDER BY attendance_id) INTO examples
    FROM (
        SELECT ownership.attendance_id
        FROM teacher_attendance_school_ownership ownership
        JOIN teacher_attendance a ON a.id = ownership.attendance_id
        WHERE ownership.school_id IS NULL OR ownership.missing_resource OR ownership.conflicting_context
           OR (EXISTS (SELECT 1 FROM school_memberships m WHERE m.user_id = a.teacher_id)
               AND NOT EXISTS (
                   SELECT 1 FROM school_memberships m
                   JOIN school_membership_roles role ON role.membership_id = m.id
                   WHERE m.user_id = a.teacher_id AND m.school_id = ownership.school_id AND role.role = 'TEACHER'
               ))
           OR (a.substitute_teacher_id IS NOT NULL
               AND EXISTS (SELECT 1 FROM school_memberships m WHERE m.user_id = a.substitute_teacher_id)
               AND NOT EXISTS (
                   SELECT 1 FROM school_memberships m
                   JOIN school_membership_roles role ON role.membership_id = m.id
                   WHERE m.user_id = a.substitute_teacher_id AND m.school_id = ownership.school_id AND role.role = 'TEACHER'
               ))
        ORDER BY ownership.attendance_id LIMIT 10
    ) invalid;

    IF examples IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot determine TeacherAttendance School ownership for %. Canonical resources must exist and structural Schools must agree. Without structural evidence require one TEACHER membership, or no membership history and exactly one School. Teacher and substitute membership history must support the owner School. Reconcile legacy ownership before migrating.', examples;
    END IF;
END $$;

UPDATE teacher_attendance a
SET school_id = ownership.school_id
FROM teacher_attendance_school_ownership ownership
WHERE ownership.attendance_id = a.id;

ALTER TABLE teacher_attendance
    ALTER COLUMN school_id SET NOT NULL,
    ADD CONSTRAINT fk_teacher_attendance_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT;

DROP INDEX idx_teacher_attendance_unique;
CREATE UNIQUE INDEX idx_teacher_attendance_school_teacher_date ON teacher_attendance(school_id, teacher_id, date);
CREATE INDEX idx_teacher_attendance_school_date ON teacher_attendance(school_id, date);
