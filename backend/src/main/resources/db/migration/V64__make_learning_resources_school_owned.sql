ALTER TABLE learning_resources ADD COLUMN school_id BIGINT;

-- Targets provide structural ownership. Creator TEACHER membership history is
-- secondary evidence; membership status does not erase historical ownership.
CREATE TEMP TABLE learning_resource_school_ownership ON COMMIT DROP AS
WITH only_school AS (
    SELECT count(*) AS school_count, min(id) AS school_id FROM schools
)
SELECT resource.id AS resource_id,
       CASE
           WHEN targets.target_count > 0 AND targets.school_count = 1 THEN targets.school_id
           WHEN targets.target_count = 0 AND creators.school_count = 1 THEN creators.school_id
           WHEN targets.target_count = 0 AND creators.school_count = 0 AND only_school.school_count = 1 THEN only_school.school_id
       END AS school_id,
       targets.target_count,
       (targets.school_count > 1 OR targets.missing_school) AS invalid_targets
FROM learning_resources resource
CROSS JOIN only_school
CROSS JOIN LATERAL (
    SELECT count(*) AS target_count, count(DISTINCT evidence.school_id) AS school_count,
           min(evidence.school_id) AS school_id, coalesce(bool_or(evidence.school_id IS NULL), false) AS missing_school
    FROM (
        SELECT year.school_id
        FROM learning_resource_classes target
        LEFT JOIN classes clazz ON clazz.id = target.class_id
        LEFT JOIN academic_years year ON year.id = clazz.academic_year_id
        WHERE target.resource_id = resource.id
        UNION ALL
        SELECT course.school_id
        FROM learning_resource_courses target
        LEFT JOIN courses course ON course.id = target.course_id
        WHERE target.resource_id = resource.id
    ) evidence
) targets
CROSS JOIN LATERAL (
    SELECT count(DISTINCT membership.school_id) AS school_count, min(membership.school_id) AS school_id
    FROM learning_resource_teachers creator
    JOIN school_memberships membership ON membership.user_id = creator.teacher_id
    JOIN school_membership_roles role ON role.membership_id = membership.id AND role.role = 'TEACHER'
    WHERE creator.resource_id = resource.id
) creators;

-- Validate before updating any row. Flyway's PostgreSQL transaction rolls back
-- schema and data together on conflict; target associations are never rewritten.
DO $$
DECLARE
    examples TEXT;
BEGIN
    SELECT string_agg('resource ' || resource_id, ', ' ORDER BY resource_id) INTO examples
    FROM (
        SELECT ownership.resource_id
        FROM learning_resource_school_ownership ownership
        WHERE ownership.school_id IS NULL OR ownership.invalid_targets
           OR (ownership.target_count > 0 AND EXISTS (
               SELECT 1
               FROM learning_resource_teachers creator
               WHERE creator.resource_id = ownership.resource_id
                 AND EXISTS (SELECT 1 FROM school_memberships membership WHERE membership.user_id = creator.teacher_id)
                 AND NOT EXISTS (
                     SELECT 1 FROM school_memberships membership
                     JOIN school_membership_roles role ON role.membership_id = membership.id
                     WHERE membership.user_id = creator.teacher_id
                       AND membership.school_id = ownership.school_id AND role.role = 'TEACHER'
                 )
           ))
        ORDER BY ownership.resource_id LIMIT 10
    ) invalid;

    IF examples IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot determine LearningResource School ownership for %. All target Classes and Courses must agree on one School, and creator membership history must support that structural owner with TEACHER membership. Without targets require one distinct creator TEACHER membership School, or no usable creator evidence and exactly one database School. Reconcile legacy ownership before migrating.', examples;
    END IF;
END $$;

-- The obsolete FK points to resources. Unmatched comments are fixture/demo data
-- with no canonical LearningResource owner; remove only those rows. Preserve
-- matching comments and the legacy resources table without remapping any IDs.
DELETE FROM resource_comments comment
WHERE NOT EXISTS (
    SELECT 1 FROM learning_resources resource WHERE resource.id = comment.on_resource_id
);

UPDATE learning_resources resource
SET school_id = ownership.school_id
FROM learning_resource_school_ownership ownership
WHERE ownership.resource_id = resource.id;

ALTER TABLE learning_resources
    ALTER COLUMN school_id SET NOT NULL,
    ADD CONSTRAINT fk_learning_resources_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT;

CREATE INDEX idx_learning_resources_school ON learning_resources(school_id);

-- ResourceComment ownership derives through its canonical LearningResource.
-- Cascading deletion matches the existing resource/comment aggregate lifecycle.
ALTER TABLE resource_comments
    DROP CONSTRAINT fk_resource_comments_resource,
    ADD CONSTRAINT fk_resource_comments_resource FOREIGN KEY (on_resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE;
