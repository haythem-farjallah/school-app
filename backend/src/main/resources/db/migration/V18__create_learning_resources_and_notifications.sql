-- Create notification_type enum if it doesn't exist
DO $$ BEGIN
    CREATE TYPE notification_type AS ENUM (
        'GRADE_ADDED',
        'GRADE_UPDATED',
        'ANNOUNCEMENT_PUBLISHED',
        'RESOURCE_UPLOADED',
        'ENROLLMENT_APPROVED',
        'ENROLLMENT_REJECTED',
        'TRANSFER_REQUESTED',
        'TRANSFER_APPROVED',
        'TRANSFER_REJECTED',
        'ASSIGNMENT_DUE',
        'ATTENDANCE_MARKED',
        'SYSTEM_ALERT',
        'GENERAL'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create learning_resources table
CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    url VARCHAR(500) NOT NULL,
    type resource_type NOT NULL,
    thumbnail_url VARCHAR(500),
    duration INTEGER,
    is_public BOOLEAN DEFAULT TRUE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create learning_resource_teachers junction table
CREATE TABLE learning_resource_teachers (
    resource_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    PRIMARY KEY (resource_id, teacher_id),
    CONSTRAINT fk_lr_teachers_resource FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_lr_teachers_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
);

-- Create learning_resource_classes junction table
CREATE TABLE learning_resource_classes (
    resource_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (resource_id, class_id),
    CONSTRAINT fk_lr_classes_resource FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_lr_classes_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
);

-- Create learning_resource_courses junction table
CREATE TABLE learning_resource_courses (
    resource_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (resource_id, course_id),
    CONSTRAINT fk_lr_courses_resource FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_lr_courses_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Update notifications table with new columns
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type notification_type DEFAULT 'GENERAL';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS entity_type VARCHAR(100);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS entity_id BIGINT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS action_url VARCHAR(500);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;

-- Update timetables table
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20);
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS semester VARCHAR(20);
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Create timetable_classes junction table
CREATE TABLE IF NOT EXISTS timetable_classes (
    timetable_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (timetable_id, class_id),
    CONSTRAINT fk_timetable_classes_timetable FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_classes_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
);

-- Create timetable_teachers junction table
CREATE TABLE IF NOT EXISTS timetable_teachers (
    timetable_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    PRIMARY KEY (timetable_id, teacher_id),
    CONSTRAINT fk_timetable_teachers_timetable FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_teachers_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
);

-- Create timetable_rooms junction table
CREATE TABLE IF NOT EXISTS timetable_rooms (
    timetable_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    PRIMARY KEY (timetable_id, room_id),
    CONSTRAINT fk_timetable_rooms_timetable FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_rooms_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

-- Update timetable_slots table
ALTER TABLE timetable_slots ADD COLUMN IF NOT EXISTS teacher_id BIGINT;
ALTER TABLE timetable_slots ADD COLUMN IF NOT EXISTS room_id BIGINT;
ALTER TABLE timetable_slots ADD COLUMN IF NOT EXISTS timetable_id BIGINT;
ALTER TABLE timetable_slots ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE timetable_slots ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add foreign key constraints for timetable_slots
ALTER TABLE timetable_slots ADD CONSTRAINT fk_timetable_slots_teacher 
    FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE SET NULL;
ALTER TABLE timetable_slots ADD CONSTRAINT fk_timetable_slots_room 
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL;
ALTER TABLE timetable_slots ADD CONSTRAINT fk_timetable_slots_timetable 
    FOREIGN KEY (timetable_id) REFERENCES timetables(id) ON DELETE CASCADE;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_learning_resources_type ON learning_resources(type);
CREATE INDEX IF NOT EXISTS idx_learning_resources_status ON learning_resources(status);
CREATE INDEX IF NOT EXISTS idx_learning_resources_created_at ON learning_resources(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user_type ON notifications(user_id, type);
CREATE INDEX IF NOT EXISTS idx_notifications_entity ON notifications(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_timetable_slots_timetable ON timetable_slots(timetable_id);
CREATE INDEX IF NOT EXISTS idx_timetable_slots_day_period ON timetable_slots(day_of_week, period_id); 