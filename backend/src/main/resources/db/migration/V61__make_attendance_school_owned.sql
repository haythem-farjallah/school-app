ALTER TABLE attendance ADD COLUMN school_id BIGINT;

-- Each contextual relationship is evidence, including the slot's own Class and Course.
-- Membership is evidence only for rows with no Class, Course or TimetableSlot at all.
CREATE TEMP TABLE attendance_school_ownership ON COMMIT DROP AS
SELECT a.id AS attendance_id,
       count(DISTINCT evidence.school_id) FILTER (WHERE evidence.required) AS school_count,
       min(evidence.school_id) FILTER (WHERE evidence.required) AS school_id,
       bool_or(evidence.required AND evidence.school_id IS NULL) AS missing_context_school
FROM attendance a
LEFT JOIN classes c ON c.id = a.class_id
LEFT JOIN academic_years class_year ON class_year.id = c.academic_year_id
LEFT JOIN courses course ON course.id = a.course_id
LEFT JOIN timetable_slots slot ON slot.id = a.timetable_slot_id
LEFT JOIN periods period ON period.id = slot.period_id
LEFT JOIN classes slot_class ON slot_class.id = slot.for_class_id
LEFT JOIN academic_years slot_class_year ON slot_class_year.id = slot_class.academic_year_id
LEFT JOIN courses slot_course ON slot_course.id = slot.for_course_id
LEFT JOIN LATERAL (
    SELECT count(*) AS membership_count, min(m.school_id) AS school_id
    FROM school_memberships m
    WHERE m.user_id = a.user_id
) membership ON true
CROSS JOIN LATERAL (VALUES
    (a.class_id IS NOT NULL, class_year.school_id),
    (a.course_id IS NOT NULL, course.school_id),
    (a.timetable_slot_id IS NOT NULL, period.school_id),
    (slot.for_class_id IS NOT NULL, slot_class_year.school_id),
    (slot.for_course_id IS NOT NULL, slot_course.school_id),
    (a.class_id IS NULL AND a.course_id IS NULL AND a.timetable_slot_id IS NULL
        AND membership.membership_count = 1, membership.school_id)
) evidence(required, school_id)
GROUP BY a.id;

-- Refuse ambiguous or unowned history rather than selecting an arbitrary tenant.
-- Validation precedes all backfill writes; Flyway also rolls the migration back on failure.
DO $$
DECLARE
    examples TEXT;
BEGIN
    SELECT string_agg('attendance ' || attendance_id, ', ' ORDER BY attendance_id)
    INTO examples
    FROM (
        SELECT attendance_id
        FROM attendance_school_ownership
        WHERE school_count <> 1 OR missing_context_school
        ORDER BY attendance_id
        LIMIT 10
    ) invalid;

    IF examples IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot determine Attendance School ownership for %. Context Schools must agree; context-free rows require exactly one SchoolMembership. Reconcile legacy ownership before migrating.', examples;
    END IF;
END $$;

UPDATE attendance a
SET school_id = ownership.school_id
FROM attendance_school_ownership ownership
WHERE ownership.attendance_id = a.id;

ALTER TABLE attendance
    ALTER COLUMN school_id SET NOT NULL,
    ADD CONSTRAINT fk_attendance_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT;

-- School now owns rows independently of optional Class/Course/Slot context.
-- V2 required a Course, contrary to the existing entity and class/user-only workflows.
ALTER TABLE attendance ALTER COLUMN course_id DROP NOT NULL;

-- Existing attendance indexes have no School prefix; these support the scoped reads and counts.
CREATE INDEX idx_attendance_school_date ON attendance(school_id, date);
CREATE INDEX idx_attendance_school_user_date ON attendance(school_id, user_id, date);
CREATE INDEX idx_attendance_school_class_date ON attendance(school_id, class_id, date);
CREATE INDEX idx_attendance_school_course_date ON attendance(school_id, course_id, date);
CREATE INDEX idx_attendance_school_slot_date ON attendance(school_id, timetable_slot_id, date);
CREATE INDEX idx_attendance_school_user_type_date ON attendance(school_id, user_type, date);
