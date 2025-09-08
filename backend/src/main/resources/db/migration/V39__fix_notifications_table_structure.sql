-- Fix notifications table structure to match communication system requirements
-- The original V2 migration created a simple notifications table, but V32 expected a different structure
-- This migration ensures the notifications table has all required columns for the communication system

-- First, check if the notifications table exists and has the old structure
-- If it does, we need to recreate it with the proper structure

-- Drop the old notifications table if it exists with the old structure
-- We'll check if it has the 'channel' column - if not, it's the old structure
DO $$
BEGIN
    -- Check if the notifications table exists but doesn't have the channel column
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notifications')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'notifications' AND column_name = 'channel') THEN
        -- Drop the old table
        DROP TABLE IF EXISTS notifications CASCADE;
    END IF;
END $$;

-- Create the proper notifications table structure for the communication system
-- This matches the structure from V32 but with PostgreSQL syntax
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    recipient_id BIGINT NOT NULL,
    recipient_type VARCHAR(50) NOT NULL CHECK (recipient_type IN ('STUDENT', 'TEACHER', 'PARENT', 'STAFF', 'ADMIN', 'ALL_USERS', 'CLASS', 'COURSE')),
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN ('GRADE_PUBLISHED', 'ASSIGNMENT_DUE', 'ATTENDANCE_ALERT', 'ANNOUNCEMENT', 'SCHEDULE_CHANGE', 'PAYMENT_REMINDER', 'WELCOME', 'PASSWORD_RESET', 'SYSTEM_ALERT', 'CUSTOM')),
    channel VARCHAR(50) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'IN_APP', 'ALL_CHANNELS')),
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'CANCELLED')),
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    scheduled_at TIMESTAMP WITHOUT TIME ZONE,
    sent_at TIMESTAMP WITHOUT TIME ZONE,
    read_at TIMESTAMP WITHOUT TIME ZONE,
    template_id BIGINT,
    metadata TEXT, -- JSON string for additional data
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    external_id VARCHAR(255) -- For tracking with external services (Twilio, FCM, etc.)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id, recipient_type);
CREATE INDEX IF NOT EXISTS idx_notifications_notification_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_channel ON notifications(channel);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_priority ON notifications(priority);
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled_at ON notifications(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_notifications_sent_at ON notifications(sent_at);
CREATE INDEX IF NOT EXISTS idx_notifications_template_id ON notifications(template_id);
CREATE INDEX IF NOT EXISTS idx_notifications_external_id ON notifications(external_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- Additional composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_status ON notifications(recipient_id, recipient_type, status);
CREATE INDEX IF NOT EXISTS idx_notifications_type_channel ON notifications(notification_type, channel);
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled_pending ON notifications(scheduled_at, status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_status ON notifications(created_at, status);

-- Add foreign key constraint to notification_templates if the table exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notification_templates') THEN
        -- Add foreign key constraint if it doesn't exist
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                      WHERE constraint_name = 'fk_notifications_template_id' 
                      AND table_name = 'notifications') THEN
            ALTER TABLE notifications 
            ADD CONSTRAINT fk_notifications_template_id 
            FOREIGN KEY (template_id) REFERENCES notification_templates(id) ON DELETE SET NULL;
        END IF;
    END IF;
END $$;