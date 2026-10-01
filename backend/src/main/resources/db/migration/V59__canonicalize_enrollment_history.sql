-- Enrollment becomes an immutable history of Student <-> Class membership.
-- Lifecycle: ACTIVE -> COMPLETED | TRANSFERRED | WITHDRAWN (all three are terminal).

-- 1. Canonical statuses. PENDING, DROPPED and SUSPENDED are not Enrollment concepts.
ALTER TABLE enrollments DROP CONSTRAINT IF EXISTS enrollments_status_check;

UPDATE enrollments SET status = 'WITHDRAWN' WHERE status IN ('PENDING', 'DROPPED', 'SUSPENDED');

ALTER TABLE enrollments ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE enrollments ALTER COLUMN status SET NOT NULL;
ALTER TABLE enrollments ADD CONSTRAINT enrollments_status_check
    CHECK (status IN ('ACTIVE', 'COMPLETED', 'TRANSFERRED', 'WITHDRAWN'));

-- 2. A Student may return to a Class, so Student + Class is no longer unique.
ALTER TABLE enrollments DROP CONSTRAINT uc_student_class;

-- 3. Legacy rows may hold several ACTIVE Enrollments for one Student in one AcademicYear.
--    There is no way to know which one is current, so none of them stays ACTIVE.
UPDATE enrollments e
SET status = 'WITHDRAWN'
FROM classes c
WHERE c.id = e.class_id
  AND e.status = 'ACTIVE'
  AND EXISTS (
      SELECT 1
      FROM enrollments other
      JOIN classes other_class ON other_class.id = other.class_id
      WHERE other.student_id = e.student_id
        AND other.status = 'ACTIVE'
        AND other.id <> e.id
        AND other_class.academic_year_id = c.academic_year_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM enrollments e
        JOIN classes c ON c.id = e.class_id
        WHERE e.status = 'ACTIVE'
        GROUP BY e.student_id, c.academic_year_id
        HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Ambiguous ACTIVE enrollments remain after cleanup';
    END IF;
END $$;

-- 4. History invariants, enforced by the database so concurrent transactions cannot bypass them.
--    The AcademicYear is only reachable through the Class, so a partial unique index cannot express
--    "one ACTIVE Enrollment per Student per AcademicYear". The Student row lock serializes writers instead.
CREATE FUNCTION enforce_enrollment_history_invariants() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    target_academic_year BIGINT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.student_id IS DISTINCT FROM OLD.student_id OR NEW.class_id IS DISTINCT FROM OLD.class_id THEN
            RAISE EXCEPTION 'ck_enrollments_immutable_relationships: enrollment student and class cannot change'
                USING ERRCODE = '23514', TABLE = 'enrollments',
                      CONSTRAINT = 'ck_enrollments_immutable_relationships';
        END IF;

        IF OLD.status <> 'ACTIVE' AND NEW.status IS DISTINCT FROM OLD.status THEN
            RAISE EXCEPTION 'ck_enrollments_terminal_status: a % enrollment cannot become %', OLD.status, NEW.status
                USING ERRCODE = '23514', TABLE = 'enrollments',
                      CONSTRAINT = 'ck_enrollments_terminal_status';
        END IF;
    END IF;

    IF NEW.status = 'ACTIVE' THEN
        SELECT academic_year_id INTO target_academic_year FROM classes WHERE id = NEW.class_id;

        IF FOUND THEN
            PERFORM 1 FROM student WHERE id = NEW.student_id FOR UPDATE;

            IF EXISTS (
                SELECT 1
                FROM enrollments e
                JOIN classes c ON c.id = e.class_id
                WHERE e.student_id = NEW.student_id
                  AND e.status = 'ACTIVE'
                  AND c.academic_year_id = target_academic_year
                  AND e.id IS DISTINCT FROM NEW.id) THEN
                RAISE EXCEPTION 'uk_enrollments_one_active_per_academic_year: student already has an active enrollment in this academic year'
                    USING ERRCODE = '23505', TABLE = 'enrollments',
                          CONSTRAINT = 'uk_enrollments_one_active_per_academic_year';
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_enrollments_history_invariants
    BEFORE INSERT OR UPDATE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION enforce_enrollment_history_invariants();

-- 5. Transfer is an Enrollment workflow, not a separate aggregate. Its indexes are dropped with the table.
DROP TABLE transfers;

-- 6. The PostgreSQL enum became unused when V47 converted the column to VARCHAR.
DROP TYPE IF EXISTS enrollment_status;
