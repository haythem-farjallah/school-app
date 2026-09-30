CREATE TABLE school_memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_memberships_user_school UNIQUE (user_id, school_id),
    CONSTRAINT fk_school_memberships_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_school_memberships_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
    CONSTRAINT ck_school_memberships_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

-- The user-school unique index covers user lookups; school lookups need their own index.
CREATE INDEX idx_school_memberships_school ON school_memberships(school_id);

CREATE TABLE school_membership_roles (
    membership_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (membership_id, role),
    CONSTRAINT fk_school_membership_roles_membership FOREIGN KEY (membership_id)
        REFERENCES school_memberships(id) ON DELETE CASCADE,
    CONSTRAINT ck_school_membership_roles_role CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT', 'GUARDIAN'))
);

DO $$
DECLARE
    school_count BIGINT;
    legacy_school_id BIGINT;
BEGIN
    IF EXISTS (
        SELECT 1 FROM users
        WHERE role IN ('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')
          AND status IN ('ACTIVE', 'SUSPENDED')
    ) THEN
        SELECT count(*) INTO school_count FROM schools;
        IF school_count = 0 THEN
            RAISE EXCEPTION 'Cannot backfill school memberships: no schools exist; explicit legacy ownership is required';
        ELSIF school_count > 1 THEN
            RAISE EXCEPTION 'Cannot backfill school memberships: multiple schools exist; explicit legacy ownership is required';
        END IF;

        SELECT id INTO legacy_school_id FROM schools;

        INSERT INTO school_memberships(user_id, school_id, status, joined_at)
        SELECT id, legacy_school_id, status::text, COALESCE(created_at, CURRENT_TIMESTAMP)
        FROM users
        WHERE role IN ('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')
          AND status IN ('ACTIVE', 'SUSPENDED')
        ON CONFLICT (user_id, school_id) DO NOTHING;

        INSERT INTO school_membership_roles(membership_id, role)
        SELECT m.id, CASE WHEN u.role = 'PARENT' THEN 'GUARDIAN' ELSE u.role::text END
        FROM school_memberships m
        JOIN users u ON u.id = m.user_id
        WHERE m.school_id = legacy_school_id
          AND u.role IN ('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')
          AND u.status IN ('ACTIVE', 'SUSPENDED')
        ON CONFLICT (membership_id, role) DO NOTHING;
    END IF;
END $$;
