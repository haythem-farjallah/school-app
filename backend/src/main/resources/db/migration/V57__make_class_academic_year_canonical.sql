-- Remove only disposable, unowned classes and their restrictive dependencies.
-- Delete indirect dependents before their enrollment/resource parents.
DELETE FROM grades
WHERE enrollment_id IN (
    SELECT e.id FROM enrollments e JOIN classes c ON c.id = e.class_id
    WHERE c.academic_year_id IS NULL
);

DELETE FROM resource_allowed_roles
WHERE resource_id IN (
    SELECT r.id FROM resources r JOIN classes c ON c.id = r.class_id
    WHERE c.academic_year_id IS NULL
);

DELETE FROM resource_comments
WHERE on_resource_id IN (
    SELECT r.id FROM resources r JOIN classes c ON c.id = r.class_id
    WHERE c.academic_year_id IS NULL
);

DELETE FROM enrollments WHERE class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);
DELETE FROM resources WHERE class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);
DELETE FROM notes WHERE class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);
DELETE FROM teaching_assignments WHERE class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);
DELETE FROM class_students WHERE class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);
DELETE FROM class_courses WHERE class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);
DELETE FROM class_teachers WHERE class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);

-- Attendance retains its existing SET NULL behavior for deleted slots/classes.
-- Learning-resource, timetable and announcement class links already cascade.
DELETE FROM timetable_slots WHERE for_class_id IN (SELECT id FROM classes WHERE academic_year_id IS NULL);
DELETE FROM classes WHERE academic_year_id IS NULL;

ALTER TABLE classes DROP COLUMN academic_year;
ALTER TABLE classes ALTER COLUMN academic_year_id SET NOT NULL;

-- V56's RESTRICT foreign key and academic_year_id index remain authoritative.
CREATE UNIQUE INDEX uk_classes_academic_year_name ON classes (academic_year_id, lower(name));
