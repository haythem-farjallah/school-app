-- Communication System Database Migration
-- Creates tables for notifications, templates, and communication logs

-- =====================================================
-- NOTIFICATION TEMPLATES TABLE
-- =====================================================
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL UNIQUE,
    template_type VARCHAR(20) NOT NULL CHECK (template_type IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'ANNOUNCEMENT')),
    subject VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    variables TEXT, -- JSON string of available variables
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    category VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    CONSTRAINT uk_template_name_type_lang UNIQUE (template_name, template_type, language)
);

-- =====================================================
-- NOTIFICATIONS TABLE
-- =====================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL CHECK (recipient_type IN ('STUDENT', 'TEACHER', 'PARENT', 'STAFF', 'ADMIN', 'ALL_USERS', 'CLASS', 'COURSE')),
    notification_type VARCHAR(30) NOT NULL CHECK (notification_type IN ('GRADE_PUBLISHED', 'ASSIGNMENT_DUE', 'ATTENDANCE_ALERT', 'ANNOUNCEMENT', 'SCHEDULE_CHANGE', 'PAYMENT_REMINDER', 'WELCOME', 'PASSWORD_RESET', 'SYSTEM_ALERT', 'CUSTOM')),
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'IN_APP', 'ALL_CHANNELS')),
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'CANCELLED')),
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    scheduled_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    template_id BIGINT NULL,
    metadata TEXT, -- JSON string for additional data
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    error_message TEXT,
    external_id VARCHAR(255), -- For tracking with external services
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    
    FOREIGN KEY (template_id) REFERENCES notification_templates(id) ON DELETE SET NULL
);

-- =====================================================
-- COMMUNICATION LOGS TABLE
-- =====================================================
CREATE TABLE communication_logs (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    channel VARCHAR(20) CHECK (channel IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'IN_APP', 'ALL_CHANNELS')) NOT NULL,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    status VARCHAR(20) CHECK (status IN ('QUEUED', 'SENT', 'DELIVERED', 'OPENED', 'CLICKED', 'BOUNCED', 'FAILED', 'UNSUBSCRIBED')) NOT NULL,
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    opened_at TIMESTAMP NULL,
    clicked_at TIMESTAMP NULL,
    bounced_at TIMESTAMP NULL,
    external_message_id VARCHAR(255),
    provider VARCHAR(100), -- e.g., "Twilio", "SendGrid", "FCM"
    cost DECIMAL(10, 4),
    error_code VARCHAR(100),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    metadata TEXT, -- JSON for additional tracking data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- =====================================================
-- USER DEVICE TOKENS TABLE (for push notifications)
-- =====================================================
CREATE TABLE user_device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500) NOT NULL,
    platform VARCHAR(10) CHECK (platform IN ('ANDROID', 'IOS', 'WEB')) NOT NULL,
    device_info TEXT, -- JSON with device details
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    
    CONSTRAINT uk_user_device_token UNIQUE (user_id, device_token)
);

-- =====================================================
-- USER NOTIFICATION PREFERENCES TABLE
-- =====================================================
CREATE TABLE user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Notification type preferences
    grade_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    attendance_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    announcement_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    payment_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    system_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Quiet hours settings
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    quiet_hours_timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Frequency settings
    digest_frequency VARCHAR(10) CHECK (digest_frequency IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY')) DEFAULT 'NONE',
    digest_time TIME DEFAULT '09:00:00',
    
    -- Additional preferences (JSON)
    preferences_json TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
);

-- =====================================================
-- NOTIFICATION CAMPAIGNS TABLE (for bulk operations)
-- =====================================================
CREATE TABLE notification_campaigns (
    id BIGSERIAL PRIMARY KEY,
    campaign_name VARCHAR(255) NOT NULL,
    campaign_description TEXT,
    campaign_type VARCHAR(20) CHECK (campaign_type IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'MULTI_CHANNEL')) NOT NULL,
    target_audience VARCHAR(20) CHECK (target_audience IN ('ALL_USERS', 'STUDENTS', 'TEACHERS', 'PARENTS', 'STAFF', 'CUSTOM')) NOT NULL,
    target_criteria TEXT, -- JSON with targeting criteria
    
    -- Campaign content
    title VARCHAR(500),
    content TEXT,
    template_id BIGINT,
    
    -- Campaign settings
    priority VARCHAR(10) CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')) NOT NULL DEFAULT 'MEDIUM',
    scheduled_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    
    -- Campaign status
    status VARCHAR(20) CHECK (status IN ('DRAFT', 'SCHEDULED', 'RUNNING', 'COMPLETED', 'CANCELLED', 'FAILED')) NOT NULL DEFAULT 'DRAFT',
    
    -- Campaign statistics
    total_recipients INT DEFAULT 0,
    sent_count INT DEFAULT 0,
    delivered_count INT DEFAULT 0,
    opened_count INT DEFAULT 0,
    clicked_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    
    FOREIGN KEY (template_id) REFERENCES notification_templates(id) ON DELETE SET NULL
);

-- =====================================================
-- NOTIFICATION CAMPAIGN RECIPIENTS TABLE
-- =====================================================
CREATE TABLE notification_campaign_recipients (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) CHECK (recipient_type IN ('STUDENT', 'TEACHER', 'PARENT', 'STAFF', 'ADMIN')) NOT NULL,
    status VARCHAR(20) CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'OPENED', 'CLICKED', 'FAILED')) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    opened_at TIMESTAMP NULL,
    clicked_at TIMESTAMP NULL,
    error_message TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    
    FOREIGN KEY (campaign_id) REFERENCES notification_campaigns(id) ON DELETE CASCADE,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    
    CONSTRAINT uk_campaign_notification UNIQUE (campaign_id, notification_id)
);

-- =====================================================
-- SMS OPT-OUT TABLE
-- =====================================================
CREATE TABLE sms_opt_outs (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(50) NOT NULL UNIQUE,
    opted_out_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255),
    user_id BIGINT,
    
);

-- =====================================================
-- EMAIL BOUNCES TABLE
-- =====================================================
CREATE TABLE email_bounces (
    id BIGSERIAL PRIMARY KEY,
    email_address VARCHAR(255) NOT NULL,
    bounce_type VARCHAR(10) CHECK (bounce_type IN ('HARD', 'SOFT', 'COMPLAINT')) NOT NULL,
    bounce_reason VARCHAR(500),
    bounced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notification_id BIGINT,
    external_message_id VARCHAR(255),
    
    
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE SET NULL
);

-- =====================================================
-- INSERT DEFAULT NOTIFICATION TEMPLATES
-- =====================================================

-- Email Templates
INSERT INTO notification_templates (template_name, template_type, subject, content, variables, category, description) VALUES
('welcome-email', 'EMAIL', 'Welcome to {{schoolName}}!', 
'<h1>Welcome {{userName}}!</h1><p>Your account has been created successfully. Your temporary password is: <strong>{{temporaryPassword}}</strong></p><p>Please login at: <a href="{{loginUrl}}">{{loginUrl}}</a></p>', 
'["userName", "temporaryPassword", "loginUrl", "schoolName"]', 'authentication', 'Welcome email for new users'),

('password-reset', 'EMAIL', 'Password Reset Request', 
'<h2>Password Reset</h2><p>Click the link below to reset your password:</p><p><a href="{{resetUrl}}">Reset Password</a></p><p>This link expires in {{expiryTime}}.</p>', 
'["resetUrl", "expiryTime"]', 'authentication', 'Password reset email'),

('grade-notification', 'EMAIL', 'New Grade Posted for {{courseName}}', 
'<h2>Grade Notification</h2><p>A new grade has been posted for {{studentName}} in {{courseName}}:</p><p><strong>Grade: {{grade}}</strong></p><p>Date: {{date}}</p>', 
'["studentName", "courseName", "grade", "date"]', 'academic', 'Grade notification email'),

('assignment-reminder', 'EMAIL', 'Assignment Due Soon: {{assignmentTitle}}', 
'<h2>Assignment Reminder</h2><p>This is a reminder that the assignment "{{assignmentTitle}}" is due on {{dueDate}}.</p><p>Please submit your work on time.</p><p><a href="{{portalUrl}}">Go to Portal</a></p>', 
'["assignmentTitle", "dueDate", "portalUrl"]', 'academic', 'Assignment due reminder'),

('attendance-alert', 'EMAIL', 'Attendance Alert for {{studentName}}', 
'<h2>Attendance Alert</h2><p>{{studentName}} was marked as {{alertType}} on {{date}}.</p><p>Please contact the school if you have any questions.</p>', 
'["studentName", "alertType", "date"]', 'attendance', 'Attendance alert email'),

('announcement', 'EMAIL', '{{title}}', 
'<h2>{{title}}</h2><div>{{content}}</div><p><em>Posted on {{date}}</em></p>', 
'["title", "content", "date"]', 'communication', 'General announcement email');

-- SMS Templates
INSERT INTO notification_templates (template_name, template_type, subject, content, variables, category, description) VALUES
('otp-sms', 'SMS', 'OTP Code', 
'Your {{schoolName}} verification code is: {{otp}}. Valid for {{expiryMinutes}} minutes. Do not share this code.', 
'["otp", "expiryMinutes", "schoolName"]', 'authentication', 'OTP verification SMS'),

('grade-notification-sms', 'SMS', 'Grade Posted', 
'New grade for {{studentName}} in {{courseName}}: {{grade}}. Check the portal for details.', 
'["studentName", "courseName", "grade"]', 'academic', 'Grade notification SMS'),

('assignment-reminder-sms', 'SMS', 'Assignment Due', 
'Reminder: "{{assignmentTitle}}" is due on {{dueDate}}. Submit on time!', 
'["assignmentTitle", "dueDate"]', 'academic', 'Assignment reminder SMS'),

('attendance-alert-sms', 'SMS', 'Attendance Alert', 
'{{studentName}} was {{alertType}} today ({{date}}). Contact school if needed.', 
'["studentName", "alertType", "date"]', 'attendance', 'Attendance alert SMS'),

('payment-reminder-sms', 'SMS', 'Payment Due', 
'Payment reminder for {{studentName}}: {{amount}} due on {{dueDate}}. Please pay on time.', 
'["studentName", "amount", "dueDate"]', 'financial', 'Payment reminder SMS'),

('announcement-sms', 'SMS', 'Announcement', 
'{{title}}: {{content}}', 
'["title", "content"]', 'communication', 'Announcement SMS');

-- Push Notification Templates
INSERT INTO notification_templates (template_name, template_type, subject, content, variables, category, description) VALUES
('grade-notification-push', 'PUSH_NOTIFICATION', 'New Grade: {{courseName}}', 
'{{grade}} posted for {{courseName}}', 
'["courseName", "grade"]', 'academic', 'Grade notification push'),

('assignment-reminder-push', 'PUSH_NOTIFICATION', 'Assignment Due Soon', 
'"{{assignmentTitle}}" due {{dueDate}}', 
'["assignmentTitle", "dueDate"]', 'academic', 'Assignment reminder push'),

('attendance-alert-push', 'PUSH_NOTIFICATION', 'Attendance Alert', 
'{{studentName}} was {{alertType}} today', 
'["studentName", "alertType"]', 'attendance', 'Attendance alert push'),

('announcement-push', 'PUSH_NOTIFICATION', '{{title}}', 
'{{content}}', 
'["title", "content"]', 'communication', 'Announcement push notification');

-- =====================================================
-- CREATE INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for common queries
CREATE INDEX idx_notifications_recipient_status ON notifications(recipient_id, recipient_type, status);
CREATE INDEX idx_notifications_type_channel ON notifications(notification_type, channel);
CREATE INDEX idx_notifications_scheduled_pending ON notifications(scheduled_at, status);
CREATE INDEX idx_communication_logs_notification_status ON communication_logs(notification_id, status);
CREATE INDEX idx_communication_logs_channel_sent ON communication_logs(channel, sent_at);

-- Indexes for analytics queries
CREATE INDEX idx_notifications_created_status ON notifications(created_at, status);
CREATE INDEX idx_communication_logs_sent_status ON communication_logs(sent_at, status);
CREATE INDEX idx_communication_logs_provider_cost ON communication_logs(provider, cost);

-- =====================================================
-- CREATE VIEWS FOR COMMON QUERIES
-- =====================================================

-- View for notification analytics
CREATE VIEW notification_analytics AS
SELECT 
    created_at::date as date,
    channel,
    status,
    COUNT(*) as count,
    AVG(CASE WHEN sent_at IS NOT NULL THEN EXTRACT(EPOCH FROM (sent_at - created_at)) END) as avg_send_time_seconds
FROM notifications 
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY created_at::date, channel, status;

-- View for communication costs
CREATE VIEW communication_costs AS
SELECT 
    sent_at::date as date,
    channel,
    provider,
    COUNT(*) as message_count,
    SUM(cost) as total_cost,
    AVG(cost) as avg_cost
FROM communication_logs 
WHERE sent_at IS NOT NULL AND cost IS NOT NULL
GROUP BY sent_at::date, channel, provider;

-- View for user notification summary
CREATE VIEW user_notification_summary AS
SELECT 
    n.recipient_id,
    n.recipient_type,
    COUNT(*) as total_notifications,
    SUM(CASE WHEN n.status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered_count,
    SUM(CASE WHEN n.status = 'READ' THEN 1 ELSE 0 END) as read_count,
    SUM(CASE WHEN n.status = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
    MAX(n.created_at) as last_notification_at
FROM notifications n
GROUP BY n.recipient_id, n.recipient_type;
