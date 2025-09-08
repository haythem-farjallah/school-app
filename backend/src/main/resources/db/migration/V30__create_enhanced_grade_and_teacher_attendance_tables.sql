-- Create enhanced_grades table
CREATE TABLE IF NOT EXISTS enhanced_grades (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    student_first_name VARCHAR(255),
    student_last_name VARCHAR(255),
    student_email VARCHAR(255),
    class_id BIGINT NOT NULL,
    class_name VARCHAR(255),
    course_id BIGINT NOT NULL,
    course_name VARCHAR(255),
    course_code VARCHAR(50),
    course_coefficient DECIMAL(3,1),
    teacher_id BIGINT NOT NULL,
    teacher_first_name VARCHAR(255),
    teacher_last_name VARCHAR(255),
    teacher_email VARCHAR(255),
    exam_type VARCHAR(50) NOT NULL CHECK (exam_type IN ('FIRST_EXAM', 'SECOND_EXAM', 'FINAL_EXAM', 'QUIZ', 'ASSIGNMENT', 'PROJECT', 'PARTICIPATION')),
    semester VARCHAR(20) NOT NULL CHECK (semester IN ('FIRST', 'SECOND', 'THIRD', 'FINAL')),
    score DECIMAL(5,2) NOT NULL,
    max_score DECIMAL(5,2) NOT NULL,
    percentage DECIMAL(5,2),
    teacher_remarks TEXT,
    teacher_signature VARCHAR(255),
    graded_at TIMESTAMP,
    is_approved BOOLEAN DEFAULT FALSE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create teacher_attendance table
CREATE TABLE IF NOT EXISTS teacher_attendance (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    teacher_first_name VARCHAR(255),
    teacher_last_name VARCHAR(255),
    teacher_email VARCHAR(255),
    date DATE NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'SICK_LEAVE', 'PERSONAL_LEAVE', 'PROFESSIONAL_DEVELOPMENT', 'SUBSTITUTE_ARRANGED')),
    course_id BIGINT,
    course_name VARCHAR(255),
    class_id BIGINT,
    class_name VARCHAR(255),
    remarks TEXT,
    excuse TEXT,
    substitute_teacher_id BIGINT,
    substitute_teacher_name VARCHAR(255),
    recorded_by_id BIGINT NOT NULL,
    recorded_by_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for enhanced_grades
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_student_id ON enhanced_grades(student_id);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_class_id ON enhanced_grades(class_id);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_course_id ON enhanced_grades(course_id);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_teacher_id ON enhanced_grades(teacher_id);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_semester ON enhanced_grades(semester);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_exam_type ON enhanced_grades(exam_type);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_student_semester ON enhanced_grades(student_id, semester);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_class_semester ON enhanced_grades(class_id, semester);
CREATE INDEX IF NOT EXISTS idx_enhanced_grades_teacher_class_course ON enhanced_grades(teacher_id, class_id, course_id);

-- Create unique constraint to prevent duplicate grades
CREATE UNIQUE INDEX IF NOT EXISTS idx_enhanced_grades_unique 
ON enhanced_grades(student_id, course_id, exam_type, semester);

-- Create indexes for teacher_attendance
CREATE INDEX IF NOT EXISTS idx_teacher_attendance_teacher_id ON teacher_attendance(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_attendance_date ON teacher_attendance(date);
CREATE INDEX IF NOT EXISTS idx_teacher_attendance_status ON teacher_attendance(status);
CREATE INDEX IF NOT EXISTS idx_teacher_attendance_teacher_date ON teacher_attendance(teacher_id, date);

-- Create unique constraint to prevent duplicate attendance records
CREATE UNIQUE INDEX IF NOT EXISTS idx_teacher_attendance_unique 
ON teacher_attendance(teacher_id, date);

-- Add foreign key constraints if the referenced tables exist
-- Note: These constraints should be added based on your actual table structure

-- Add comments for documentation
COMMENT ON TABLE enhanced_grades IS 'Enhanced grade records with detailed exam types, semesters, and teacher information';
COMMENT ON TABLE teacher_attendance IS 'Teacher attendance records separate from student attendance';

COMMENT ON COLUMN enhanced_grades.exam_type IS 'Type of examination: FIRST_EXAM, SECOND_EXAM, FINAL_EXAM, QUIZ, ASSIGNMENT, PROJECT, PARTICIPATION';
COMMENT ON COLUMN enhanced_grades.semester IS 'Academic semester: FIRST, SECOND, THIRD, FINAL';
COMMENT ON COLUMN enhanced_grades.percentage IS 'Calculated percentage score (score/max_score * 100)';
COMMENT ON COLUMN enhanced_grades.is_approved IS 'Whether the grade has been approved by staff';

COMMENT ON COLUMN teacher_attendance.status IS 'Attendance status: PRESENT, ABSENT, LATE, SICK_LEAVE, PERSONAL_LEAVE, PROFESSIONAL_DEVELOPMENT, SUBSTITUTE_ARRANGED';
COMMENT ON COLUMN teacher_attendance.substitute_teacher_id IS 'ID of substitute teacher if applicable';
