/* ---------- 1. Permission catalogue ---------- */
ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS code VARCHAR(60);

INSERT INTO permissions (code, description) VALUES
    /* student domain */
                                                ('STUDENT_READ',     'Read student profile'),
                                                ('STUDENT_CREATE',   'Create student'),
                                                ('STUDENT_UPDATE',   'Update student'),
                                                ('STUDENT_DELETE',   'Delete student'),
    /* teacher domain */
                                                ('TEACHER_READ',     'Read teacher profile'),
                                                ('TEACHER_CREATE',   'Create teacher'),
                                                ('TEACHER_UPDATE',   'Update teacher'),
                                                ('TEACHER_DELETE',   'Delete teacher'),
    /* grades */
                                                ('GRADE_READ',       'Read grades'),
                                                ('GRADE_WRITE',      'Edit grades');

/* ---------- 2. Role-default bridge ---------- */
CREATE TABLE role_permissions (
                                  role          VARCHAR(30) NOT NULL,
                                  permission_id BIGINT      NOT NULL,
                                  PRIMARY KEY (role, permission_id),
                                  CONSTRAINT fk_roleperm_perm
                                      FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

/* TEACHER defaults: read students & grades, write grades */
INSERT INTO role_permissions (role, permission_id)
SELECT 'TEACHER', id FROM permissions
WHERE code IN ('STUDENT_READ','GRADE_READ','GRADE_WRITE');

/* WORKER defaults: full CRUD on students + read teachers */
INSERT INTO role_permissions (role, permission_id)
SELECT 'WORKER', id FROM permissions
WHERE code IN (
               'STUDENT_READ','STUDENT_CREATE','STUDENT_UPDATE','STUDENT_DELETE',
               'TEACHER_READ'
    );

/* ADMIN inherits everything through role hierarchy later */
