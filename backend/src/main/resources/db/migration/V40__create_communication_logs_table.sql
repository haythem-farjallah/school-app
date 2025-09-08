-- Create communication_logs table for tracking email delivery status and analytics
-- This table was missing from the database schema but is required by the communication system

CREATE TABLE IF NOT EXISTS communication_logs (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    notification_id BIGINT NOT NULL,
    channel VARCHAR(50) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'IN_APP', 'ALL_CHANNELS')),
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL CHECK (status IN ('QUEUED', 'SENT', 'DELIVERED', 'OPENED', 'CLICKED', 'BOUNCED', 'FAILED', 'UNSUBSCRIBED')),
    sent_at TIMESTAMP WITHOUT TIME ZONE,
    delivered_at TIMESTAMP WITHOUT TIME ZONE,
    opened_at TIMESTAMP WITHOUT TIME ZONE,
    clicked_at TIMESTAMP WITHOUT TIME ZONE,
    bounced_at TIMESTAMP WITHOUT TIME ZONE,
    external_message_id VARCHAR(255),
    provider VARCHAR(100), -- e.g., "Twilio", "SendGrid", "FCM"
    cost DOUBLE PRECISION,
    error_code VARCHAR(100),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    metadata TEXT -- JSON for additional tracking data
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_communication_logs_notification_id ON communication_logs(notification_id);
CREATE INDEX IF NOT EXISTS idx_communication_logs_channel ON communication_logs(channel);
CREATE INDEX IF NOT EXISTS idx_communication_logs_status ON communication_logs(status);
CREATE INDEX IF NOT EXISTS idx_communication_logs_sent_at ON communication_logs(sent_at);
CREATE INDEX IF NOT EXISTS idx_communication_logs_delivered_at ON communication_logs(delivered_at);
CREATE INDEX IF NOT EXISTS idx_communication_logs_external_message_id ON communication_logs(external_message_id);
CREATE INDEX IF NOT EXISTS idx_communication_logs_provider ON communication_logs(provider);
CREATE INDEX IF NOT EXISTS idx_communication_logs_recipient_email ON communication_logs(recipient_email);
CREATE INDEX IF NOT EXISTS idx_communication_logs_recipient_phone ON communication_logs(recipient_phone);

-- Additional composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_communication_logs_notification_status ON communication_logs(notification_id, status);
CREATE INDEX IF NOT EXISTS idx_communication_logs_channel_sent ON communication_logs(channel, sent_at);
CREATE INDEX IF NOT EXISTS idx_communication_logs_sent_status ON communication_logs(sent_at, status);
CREATE INDEX IF NOT EXISTS idx_communication_logs_provider_cost ON communication_logs(provider, cost);

-- Add foreign key constraint to notifications table
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                  WHERE constraint_name = 'fk_communication_logs_notification_id' 
                  AND table_name = 'communication_logs') THEN
        ALTER TABLE communication_logs 
        ADD CONSTRAINT fk_communication_logs_notification_id 
        FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE;
    END IF;
END $$;
