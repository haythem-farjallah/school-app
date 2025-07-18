-- Enhance attendance table with new fields for better tracking
ALTER TABLE attendance 
ADD COLUMN IF NOT EXISTS class_id BIGINT,
ADD COLUMN IF NOT EXISTS timetable_slot_id BIGINT,
ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PRESENT',
ADD COLUMN IF NOT EXISTS user_type VARCHAR(10),
ADD COLUMN IF NOT EXISTS remarks TEXT,
ADD COLUMN IF NOT EXISTS excuse TEXT,
ADD COLUMN IF NOT EXISTS medical_note TEXT,
ADD COLUMN IF NOT EXISTS recorded_by_id BIGINT,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add foreign key constraints
ALTER TABLE attendance 
ADD CONSTRAINT fk_attendance_class 
FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL;

ALTER TABLE attendance 
ADD CONSTRAINT fk_attendance_timetable_slot 
FOREIGN KEY (timetable_slot_id) REFERENCES timetable_slots(id) ON DELETE SET NULL;

ALTER TABLE attendance 
ADD CONSTRAINT fk_attendance_recorded_by 
FOREIGN KEY (recorded_by_id) REFERENCES users(id) ON DELETE SET NULL;

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_attendance_user_date ON attendance(user_id, date);
CREATE INDEX IF NOT EXISTS idx_attendance_class_date ON attendance(class_id, date);
CREATE INDEX IF NOT EXISTS idx_attendance_course_date ON attendance(course_id, date);
CREATE INDEX IF NOT EXISTS idx_attendance_user_type_date ON attendance(user_type, date);
CREATE INDEX IF NOT EXISTS idx_attendance_status ON attendance(status);
CREATE INDEX IF NOT EXISTS idx_attendance_timetable_slot_date ON attendance(timetable_slot_id, date);

-- Update existing records to have default values
UPDATE attendance SET 
    status = CASE 
        WHEN present = true THEN 'PRESENT' 
        ELSE 'ABSENT' 
    END,
    user_type = 'STUDENT',
    recorded_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE status IS NULL;

-- Drop the old present column since we now use status
ALTER TABLE attendance DROP COLUMN IF EXISTS present; 