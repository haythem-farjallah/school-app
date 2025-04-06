-- Flyway Migration: V2__Initial.sql
-- PostgreSQL version

-- ================================
-- USER & AUTHORIZATION MODULE
-- ================================

-- Base user table
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       email VARCHAR(255) UNIQUE,
                       telephone VARCHAR(50),
                       birthday DATE,
                       gender VARCHAR(20),
                       address VARCHAR(255),
                       password VARCHAR(255),
                       image VARCHAR(255)
);

-- Permissions table
CREATE TABLE permissions (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(255),
                             description VARCHAR(255)
);

-- User-Permissions join table
CREATE TABLE user_permissions (
                                  user_id BIGINT NOT NULL,
                                  permission_id BIGINT NOT NULL,
                                  PRIMARY KEY (user_id, permission_id),
                                  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id),
                                  CONSTRAINT fk_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- Profile Settings
CREATE TABLE profile_settings (
                                  id BIGSERIAL PRIMARY KEY,
                                  language VARCHAR(50),
                                  theme VARCHAR(50),
                                  notifications_enabled BOOLEAN,
                                  dark_mode BOOLEAN,
                                  user_id BIGINT,
                                  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Inheritance Tables for specific user types

-- Administration
CREATE TABLE administration (
                                id BIGINT PRIMARY KEY,
                                department VARCHAR(255),
                                job_title VARCHAR(255),
                                CONSTRAINT fk_admin_user FOREIGN KEY (id) REFERENCES users(id)
);

-- Teacher
CREATE TABLE teacher (
                         id BIGINT PRIMARY KEY,
                         qualifications VARCHAR(255),
                         subjects_taught VARCHAR(255),
                         available_hours INT,
                         schedule_preferences VARCHAR(255),
                         CONSTRAINT fk_teacher_user FOREIGN KEY (id) REFERENCES users(id)
);

-- Student
CREATE TABLE student (
                         id BIGINT PRIMARY KEY,
                         grade_level VARCHAR(50),
                         enrollment_year INT,
                         CONSTRAINT fk_student_user FOREIGN KEY (id) REFERENCES users(id)
);

-- Parent
CREATE TABLE parent (
                        id BIGINT PRIMARY KEY,
                        preferred_contact_method VARCHAR(50),
                        CONSTRAINT fk_parent_user FOREIGN KEY (id) REFERENCES users(id)
);

-- Worker
CREATE TABLE worker (
                        id BIGINT PRIMARY KEY,
                        job_role VARCHAR(255),
                        department VARCHAR(255),
                        CONSTRAINT fk_worker_user FOREIGN KEY (id) REFERENCES users(id)
);

-- Join table for Parent-Student relationship
CREATE TABLE parent_students (
                                 parent_id BIGINT NOT NULL,
                                 student_id BIGINT NOT NULL,
                                 PRIMARY KEY (parent_id, student_id),
                                 CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES parent(id),
                                 CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES student(id)
);

-- ================================
-- ACADEMIC MANAGEMENT MODULE
-- ================================

-- Levels table (for academic stages)
CREATE TABLE levels (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255)
);

-- Schedule table
CREATE TABLE schedules (
                           id BIGSERIAL PRIMARY KEY
);

-- Courses table
CREATE TABLE courses (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255),
                         color VARCHAR(50) DEFAULT '#9660EB',
                         coefficient DOUBLE PRECISION,
                         teacher_id BIGINT,
                         CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id)
);

-- Classes table (named as classes; entity named ClassEntity in code)
CREATE TABLE classes (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255),
                         level_id BIGINT,
                         schedule_id BIGINT,
                         CONSTRAINT fk_class_level FOREIGN KEY (level_id) REFERENCES levels(id),
                         CONSTRAINT fk_class_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id)
);

-- Join table for Class - Student relationship
CREATE TABLE class_students (
                                class_id BIGINT NOT NULL,
                                student_id BIGINT NOT NULL,
                                PRIMARY KEY (class_id, student_id),
                                CONSTRAINT fk_class FOREIGN KEY (class_id) REFERENCES classes(id),
                                CONSTRAINT fk_class_student FOREIGN KEY (student_id) REFERENCES student(id)
);

-- Join table for Class - Course relationship
CREATE TABLE class_courses (
                               class_id BIGINT NOT NULL,
                               course_id BIGINT NOT NULL,
                               PRIMARY KEY (class_id, course_id),
                               CONSTRAINT fk_cc_class FOREIGN KEY (class_id) REFERENCES classes(id),
                               CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- Join table for Schedule - Course relationship
CREATE TABLE schedule_courses (
                                  schedule_id BIGINT NOT NULL,
                                  course_id BIGINT NOT NULL,
                                  PRIMARY KEY (schedule_id, course_id),
                                  CONSTRAINT fk_sc_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id),
                                  CONSTRAINT fk_sc_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- Resources table
CREATE TABLE resources (
                           id BIGSERIAL PRIMARY KEY,
                           url VARCHAR(255),
                           title VARCHAR(255),
                           description TEXT,
                           is_public BOOLEAN,
                           class_id BIGINT,
                           level_id BIGINT,
                           CONSTRAINT fk_resource_class FOREIGN KEY (class_id) REFERENCES classes(id),
                           CONSTRAINT fk_resource_level FOREIGN KEY (level_id) REFERENCES levels(id)
);

-- Join table for Resource allowed roles
CREATE TABLE resource_allowed_roles (
                                        resource_id BIGINT NOT NULL,
                                        role VARCHAR(50) NOT NULL,
                                        PRIMARY KEY (resource_id, role),
                                        CONSTRAINT fk_resource_role FOREIGN KEY (resource_id) REFERENCES resources(id)
);

-- ================================
-- OPERATIONAL & ASSESSMENT MODULE
-- ================================

-- Semesters table
CREATE TABLE semesters (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(255),
                           start_date DATE,
                           end_date DATE
);

-- Bulletins table (report cards)
CREATE TABLE bulletins (
                           id BIGSERIAL PRIMARY KEY,
                           student_id BIGINT NOT NULL,
                           semester_id BIGINT NOT NULL,
                           overall_average DOUBLE PRECISION,
                           CONSTRAINT fk_bulletin_student FOREIGN KEY (student_id) REFERENCES student(id),
                           CONSTRAINT fk_bulletin_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

-- Subject Grades table
CREATE TABLE subject_grades (
                                id BIGSERIAL PRIMARY KEY,
                                course_id BIGINT NOT NULL,
                                student_id BIGINT NOT NULL,
                                teacher_id BIGINT NOT NULL,
                                semester_id BIGINT NOT NULL,
                                note1 DOUBLE PRECISION,
                                note2 DOUBLE PRECISION,
                                note3 DOUBLE PRECISION,
                                coefficient DOUBLE PRECISION,
                                final_note DOUBLE PRECISION,
                                bulletin_id BIGINT,
                                CONSTRAINT fk_subj_course FOREIGN KEY (course_id) REFERENCES courses(id),
                                CONSTRAINT fk_subj_student FOREIGN KEY (student_id) REFERENCES student(id),
                                CONSTRAINT fk_subj_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id),
                                CONSTRAINT fk_subj_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
                                CONSTRAINT fk_subj_bulletin FOREIGN KEY (bulletin_id) REFERENCES bulletins(id)
);

-- Notes table (periodic assessments)
CREATE TABLE notes (
                       id BIGSERIAL PRIMARY KEY,
                       course_id BIGINT NOT NULL,
                       class_id BIGINT NOT NULL,
                       teacher_id BIGINT NOT NULL,
                       student_id BIGINT NOT NULL,
                       note1 DOUBLE PRECISION,
                       note2 DOUBLE PRECISION,
                       CONSTRAINT fk_note_course FOREIGN KEY (course_id) REFERENCES courses(id),
                       CONSTRAINT fk_note_class FOREIGN KEY (class_id) REFERENCES classes(id),
                       CONSTRAINT fk_note_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id),
                       CONSTRAINT fk_note_student FOREIGN KEY (student_id) REFERENCES student(id)
);

-- Attendance table
CREATE TABLE attendance (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            course_id BIGINT NOT NULL,
                            date TIMESTAMP WITHOUT TIME ZONE,
                            present BOOLEAN,
                            CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users(id),
                            CONSTRAINT fk_attendance_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- Notifications table
CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               message VARCHAR(255),
                               read_status BOOLEAN,
                               created_at TIMESTAMP WITHOUT TIME ZONE,
                               CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Timetables table
CREATE TABLE timetables (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            timetable_json TEXT,
                            CONSTRAINT fk_timetable_user FOREIGN KEY (user_id) REFERENCES users(id)
);
