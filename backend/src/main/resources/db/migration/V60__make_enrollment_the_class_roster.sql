-- Enrollment is the only Student <-> Class membership: the current roster of a Class is its ACTIVE Enrollments.
-- class_students was a second, history-less copy of that relationship.

-- A legacy link carries no enrollment date, status history or academic-year intent, so it cannot be turned into
-- an Enrollment without inventing history, and it must not be dropped while it disagrees with Enrollment.
-- It is only redundant when an ACTIVE Enrollment for the same student and class already exists.
DO $$
DECLARE
    inconsistent BIGINT;
    examples TEXT;
BEGIN
    SELECT count(*),
           string_agg('(class ' || class_id || ', student ' || student_id || ')', ', ')
    INTO inconsistent, examples
    FROM (
        SELECT cs.class_id, cs.student_id
        FROM class_students cs
        WHERE NOT EXISTS (
            SELECT 1 FROM enrollments e
            WHERE e.class_id = cs.class_id AND e.student_id = cs.student_id AND e.status = 'ACTIVE')
        ORDER BY cs.class_id, cs.student_id
        LIMIT 10) first_rows;

    IF inconsistent > 0 THEN
        RAISE EXCEPTION 'Cannot drop class_students: legacy roster rows have no matching ACTIVE enrollment, e.g. %. Reconcile class_students with enrollments before migrating.', examples;
    END IF;
END $$;

DROP TABLE class_students;
