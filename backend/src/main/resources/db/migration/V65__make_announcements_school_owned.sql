ALTER TABLE announcements ADD COLUMN school_id BIGINT;

-- Class targets are structural ownership. Only the creator's account-role-matching
-- TEACHER/ADMIN membership history provides secondary evidence, regardless of status.
-- Staff creators and publishers provide no School ownership evidence.
CREATE TEMP TABLE announcement_school_ownership ON COMMIT DROP AS
WITH only_school AS (
    SELECT count(*) AS school_count, min(id) AS school_id FROM schools
)
SELECT announcement.id AS announcement_id,
       CASE
           WHEN targets.target_count > 0 AND targets.school_count = 1 THEN targets.school_id
           WHEN targets.target_count = 0 AND creators.school_count = 1 THEN creators.school_id
           WHEN targets.target_count = 0 AND creators.school_count = 0 AND only_school.school_count = 1 THEN only_school.school_id
       END AS school_id,
       targets.target_count,
       (targets.school_count > 1 OR targets.missing_school) AS invalid_targets
FROM announcements announcement
CROSS JOIN only_school
CROSS JOIN LATERAL (
    SELECT count(*) AS target_count, count(DISTINCT year.school_id) AS school_count,
           min(year.school_id) AS school_id,
           coalesce(bool_or(year.school_id IS NULL OR school.id IS NULL), false) AS missing_school
    FROM announcement_target_classes target
    LEFT JOIN classes clazz ON clazz.id = target.class_id
    LEFT JOIN academic_years year ON year.id = clazz.academic_year_id
    LEFT JOIN schools school ON school.id = year.school_id
    WHERE target.announcement_id = announcement.id
) targets
CROSS JOIN LATERAL (
    SELECT count(DISTINCT membership.school_id) AS school_count, min(membership.school_id) AS school_id
    FROM users creator
    JOIN school_memberships membership ON membership.user_id = creator.id
    JOIN school_membership_roles role ON role.membership_id = membership.id AND role.role = creator.role::text
    WHERE creator.id = announcement.created_by_id AND creator.role IN ('TEACHER', 'ADMIN')
) creators;

-- Validate every row before backfilling. PostgreSQL/Flyway rolls schema and data
-- back together on conflict; no Announcement or target association is discarded.
DO $$
DECLARE
    examples TEXT;
BEGIN
    SELECT string_agg('announcement ' || announcement_id, ', ' ORDER BY announcement_id) INTO examples
    FROM (
        SELECT ownership.announcement_id
        FROM announcement_school_ownership ownership
        JOIN announcements announcement ON announcement.id = ownership.announcement_id
        WHERE ownership.school_id IS NULL OR ownership.invalid_targets
           OR (ownership.target_count > 0 AND EXISTS (
               SELECT 1 FROM users creator
               WHERE creator.id = announcement.created_by_id AND creator.role IN ('TEACHER', 'ADMIN')
                 AND EXISTS (SELECT 1 FROM school_memberships membership WHERE membership.user_id = creator.id)
                 AND NOT EXISTS (
                     SELECT 1 FROM school_memberships membership
                     JOIN school_membership_roles role ON role.membership_id = membership.id
                     WHERE membership.user_id = creator.id AND membership.school_id = ownership.school_id
                       AND role.role = creator.role::text
                 )
           ))
        ORDER BY ownership.announcement_id LIMIT 10
    ) invalid;

    IF examples IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot determine Announcement School ownership for %. All target Classes must agree on one School, and TEACHER/ADMIN creator membership history must support that structural owner with the matching role. Without targets require one distinct applicable creator membership School, or no usable creator evidence and exactly one database School. Staff and publishers are not ownership evidence. Reconcile legacy ownership before migrating.', examples;
    END IF;
END $$;

UPDATE announcements announcement
SET school_id = ownership.school_id
FROM announcement_school_ownership ownership
WHERE ownership.announcement_id = announcement.id;

ALTER TABLE announcements
    ALTER COLUMN school_id SET NOT NULL,
    ADD CONSTRAINT fk_announcements_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT;

CREATE INDEX idx_announcements_school ON announcements(school_id);
