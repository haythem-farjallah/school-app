-- Enhance timetable system for smart optimization
-- Add indexes for better performance

-- Add indexes to timetable_slots for optimization queries
CREATE INDEX IF NOT EXISTS idx_timetable_slots_teacher_day_period 
ON timetable_slots(teacher_id, day_of_week, period_id);

CREATE INDEX IF NOT EXISTS idx_timetable_slots_room_day_period 
ON timetable_slots(room_id, day_of_week, period_id);

CREATE INDEX IF NOT EXISTS idx_timetable_slots_class_day_period 
ON timetable_slots(for_class_id, day_of_week, period_id);

-- Add teacher preferences column if not exists
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS schedule_preferences TEXT;
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS weekly_capacity INTEGER DEFAULT 40;

-- Add room capacity if not exists
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS capacity INTEGER DEFAULT 30;

-- Add course optimization fields
ALTER TABLE courses ADD COLUMN IF NOT EXISTS difficulty_level INTEGER DEFAULT 1;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS requires_special_equipment BOOLEAN DEFAULT FALSE;

-- Create timetable_optimization_logs table for tracking optimizations
CREATE TABLE IF NOT EXISTS timetable_optimization_logs (
    id BIGSERIAL PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    optimization_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    final_score DECIMAL(5,2),
    hard_constraint_score DECIMAL(5,2),
    soft_constraint_score DECIMAL(5,2),
    improvement_percentage DECIMAL(5,2),
    optimization_parameters TEXT, -- JSON
    result_summary TEXT, -- JSON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_optimization_logs_timetable FOREIGN KEY (timetable_id) REFERENCES timetables(id)
);

-- Create teacher_workload_analysis table for caching workload analyses
CREATE TABLE IF NOT EXISTS teacher_workload_analysis (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    analysis_date DATE NOT NULL,
    total_weekly_hours INTEGER NOT NULL,
    max_weekly_capacity INTEGER NOT NULL,
    workload_percentage DECIMAL(5,2) NOT NULL,
    workload_status VARCHAR(50) NOT NULL,
    total_gaps INTEGER DEFAULT 0,
    max_consecutive_hours INTEGER DEFAULT 0,
    schedule_efficiency DECIMAL(3,2) DEFAULT 0.0,
    analysis_data TEXT, -- JSON with detailed breakdown
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_workload_analysis_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT uc_teacher_analysis_date UNIQUE (teacher_id, analysis_date)
);

-- Create timetable_conflicts table for tracking conflicts
CREATE TABLE IF NOT EXISTS timetable_conflicts (
    id BIGSERIAL PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    conflict_type VARCHAR(100) NOT NULL,
    conflict_severity VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    affected_slots TEXT, -- JSON array of slot IDs
    affected_teachers TEXT, -- JSON array of teacher IDs
    affected_classes TEXT, -- JSON array of class IDs
    affected_rooms TEXT, -- JSON array of room IDs
    resolution_status VARCHAR(50) DEFAULT 'UNRESOLVED',
    resolution_notes TEXT,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    
    CONSTRAINT fk_conflicts_timetable FOREIGN KEY (timetable_id) REFERENCES timetables(id)
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_optimization_logs_timetable_id ON timetable_optimization_logs(timetable_id);
CREATE INDEX IF NOT EXISTS idx_optimization_logs_status ON timetable_optimization_logs(status);
CREATE INDEX IF NOT EXISTS idx_optimization_logs_start_time ON timetable_optimization_logs(start_time);

CREATE INDEX IF NOT EXISTS idx_workload_analysis_teacher_id ON teacher_workload_analysis(teacher_id);
CREATE INDEX IF NOT EXISTS idx_workload_analysis_date ON teacher_workload_analysis(analysis_date);
CREATE INDEX IF NOT EXISTS idx_workload_analysis_status ON teacher_workload_analysis(workload_status);

CREATE INDEX IF NOT EXISTS idx_conflicts_timetable_id ON timetable_conflicts(timetable_id);
CREATE INDEX IF NOT EXISTS idx_conflicts_type ON timetable_conflicts(conflict_type);
CREATE INDEX IF NOT EXISTS idx_conflicts_severity ON timetable_conflicts(conflict_severity);
CREATE INDEX IF NOT EXISTS idx_conflicts_status ON timetable_conflicts(resolution_status);

-- Add comments for documentation
COMMENT ON TABLE timetable_optimization_logs IS 'Logs of timetable optimization runs with results and performance metrics';
COMMENT ON TABLE teacher_workload_analysis IS 'Cached teacher workload analyses for performance and historical tracking';
COMMENT ON TABLE timetable_conflicts IS 'Detected conflicts in timetables with resolution tracking';

COMMENT ON COLUMN teacher.schedule_preferences IS 'JSON array of preferred time slots, e.g., ["MONDAY_MORNING", "TUESDAY_AFTERNOON"]';
COMMENT ON COLUMN teacher.weekly_capacity IS 'Maximum weekly teaching hours capacity for this teacher';
COMMENT ON COLUMN courses.difficulty_level IS 'Course difficulty level (1-5) for optimization constraints';
COMMENT ON COLUMN courses.requires_special_equipment IS 'Whether this course requires special equipment or room type';
