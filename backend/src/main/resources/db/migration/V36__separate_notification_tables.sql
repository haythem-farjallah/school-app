-- Create user_notifications table for operational notifications
CREATE TABLE user_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    message VARCHAR(255),
    type notification_type DEFAULT 'GENERAL',
    entity_type VARCHAR(100),
    entity_id BIGINT,
    action_url VARCHAR(500),
    read_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT fk_user_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Note: Starting fresh with user_notifications table for operational notifications
-- The original notifications table is now dedicated to the communication system

-- Add indexes for performance
CREATE INDEX idx_user_notifications_user_id ON user_notifications(user_id);
CREATE INDEX idx_user_notifications_read_status ON user_notifications(read_status);
CREATE INDEX idx_user_notifications_created_at ON user_notifications(created_at);
CREATE INDEX idx_user_notifications_type ON user_notifications(type);
