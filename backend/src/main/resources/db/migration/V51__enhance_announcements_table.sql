-- Add new fields to announcements table for better functionality
-- V51: Enhance announcements table with target classes and creator information

-- Add creator information fields (temporary until proper migration)
ALTER TABLE announcements 
ADD COLUMN IF NOT EXISTS created_by_id BIGINT,
ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS target_type VARCHAR(50);

-- Create announcement_target_classes junction table
CREATE TABLE IF NOT EXISTS announcement_target_classes (
    announcement_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (announcement_id, class_id),
    CONSTRAINT fk_announcement_target_classes_announcement 
        FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_target_classes_class 
        FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_announcements_created_by_id ON announcements(created_by_id);
CREATE INDEX IF NOT EXISTS idx_announcements_target_type ON announcements(target_type);
CREATE INDEX IF NOT EXISTS idx_announcements_created_at ON announcements(created_at);

-- Add comments for documentation
COMMENT ON COLUMN announcements.created_by_id IS 'ID of the user who created the announcement (temporary field)';
COMMENT ON COLUMN announcements.created_by_name IS 'Name of the user who created the announcement (temporary field)';
COMMENT ON COLUMN announcements.target_type IS 'Type of targeting: CLASSES, ALL_STAFF, ALL_TEACHERS, ALL_STUDENTS, WHOLE_SCHOOL, SPECIFIC_USERS';
COMMENT ON TABLE announcement_target_classes IS 'Junction table linking announcements to target classes';
