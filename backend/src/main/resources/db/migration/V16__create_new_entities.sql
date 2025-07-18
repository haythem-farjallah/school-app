-- Create audit_events table
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_type audit_event_type NOT NULL,
    summary VARCHAR(255),
    details TEXT,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    acted_by_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    CONSTRAINT fk_audit_events_user FOREIGN KEY (acted_by_id) REFERENCES users(id)
);

-- Create resource_comments table
CREATE TABLE resource_comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    on_resource_id BIGINT NOT NULL,
    commented_by_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_resource_comments_resource FOREIGN KEY (on_resource_id) REFERENCES resources(id),
    CONSTRAINT fk_resource_comments_user FOREIGN KEY (commented_by_id) REFERENCES users(id)
);

-- Create rooms table
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INTEGER,
    room_type room_type NOT NULL
);

-- Create periods table
CREATE TABLE periods (
    id BIGSERIAL PRIMARY KEY,
    index_number INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);

-- Create timetable_slots table
CREATE TABLE timetable_slots (
    id BIGSERIAL PRIMARY KEY,
    day_of_week day_of_week NOT NULL,
    description VARCHAR(255),
    period_id BIGINT NOT NULL,
    for_class_id BIGINT,
    for_course_id BIGINT,
    
    CONSTRAINT fk_timetable_slots_period FOREIGN KEY (period_id) REFERENCES periods(id),
    CONSTRAINT fk_timetable_slots_class FOREIGN KEY (for_class_id) REFERENCES classes(id),
    CONSTRAINT fk_timetable_slots_course FOREIGN KEY (for_course_id) REFERENCES courses(id)
);

-- Create enrollments table
CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    status enrollment_status NOT NULL DEFAULT 'PENDING',
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    final_grad FLOAT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_enrollments_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT uc_student_class UNIQUE (student_id, class_id)
);

-- Create transfers table
CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    effective_at TIMESTAMP,
    reason TEXT,
    student_id BIGINT NOT NULL,
    
    CONSTRAINT fk_transfers_student FOREIGN KEY (student_id) REFERENCES student(id)
);

-- Create grades table
CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,
    content VARCHAR(255),
    score FLOAT NOT NULL,
    weight FLOAT DEFAULT 1.0,
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enrollment_id BIGINT NOT NULL,
    assigned_by_id BIGINT NOT NULL,
    
    CONSTRAINT fk_grades_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    CONSTRAINT fk_grades_teacher FOREIGN KEY (assigned_by_id) REFERENCES teacher(id)
);

-- Create announcements table
CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_public BOOLEAN DEFAULT TRUE,
    importance announcement_importance DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create staff_announcements junction table
CREATE TABLE staff_announcements (
    staff_id BIGINT NOT NULL,
    announcement_id BIGINT NOT NULL,
    
    CONSTRAINT fk_staff_announcements_staff FOREIGN KEY (staff_id) REFERENCES staff(id),
    CONSTRAINT fk_staff_announcements_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id),
    CONSTRAINT pk_staff_announcements PRIMARY KEY (staff_id, announcement_id)
);

-- Create class_teachers junction table
CREATE TABLE class_teachers (
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    
    CONSTRAINT fk_class_teachers_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_class_teachers_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT pk_class_teachers PRIMARY KEY (class_id, teacher_id)
);

-- Add room assignment to classes
ALTER TABLE classes ADD COLUMN assigned_room_id BIGINT;
ALTER TABLE classes ADD CONSTRAINT fk_classes_room FOREIGN KEY (assigned_room_id) REFERENCES rooms(id); 