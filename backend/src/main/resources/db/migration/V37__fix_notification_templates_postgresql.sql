-- Fix notification_templates table for PostgreSQL
-- The original V32 migration used MySQL syntax which failed on PostgreSQL

-- Drop the table if it exists (in case it was partially created)
DROP TABLE IF EXISTS notification_templates CASCADE;

-- Create notification_templates table with PostgreSQL syntax
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL UNIQUE,
    template_type VARCHAR(50) NOT NULL CHECK (template_type IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'ANNOUNCEMENT')),
    subject VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    variables TEXT, -- JSON string of available variables
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    category VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

-- Create indexes
CREATE INDEX idx_template_name ON notification_templates(template_name);
CREATE INDEX idx_template_type ON notification_templates(template_type);
CREATE INDEX idx_template_active ON notification_templates(is_active);
CREATE INDEX idx_template_language ON notification_templates(language);
CREATE INDEX idx_template_category ON notification_templates(category);
CREATE UNIQUE INDEX uk_template_name_type_lang ON notification_templates(template_name, template_type, language);

-- Insert default notification templates
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
'<h2>Attendance Alert</h2><p>This is to inform you about an attendance issue for {{studentName}}:</p><p><strong>Alert Type: {{alertType}}</strong></p><p>Date: {{date}}</p>', 
'["studentName", "alertType", "date"]', 'attendance', 'Attendance alert email'),

('announcement', 'EMAIL', '{{title}}', 
'<h2>{{title}}</h2><div>{{content}}</div><p>Date: {{date}}</p>', 
'["title", "content", "date"]', 'communication', 'General announcement email'),

('bulk-notification', 'EMAIL', '{{subject}}', 
'<h2>{{title}}</h2><div>{{message}}</div><p>This message was sent to: {{recipientType}}</p><p>Date: {{date}}</p>', 
'["subject", "title", "message", "recipientType", "date"]', 'communication', 'Bulk notification email'),

('status-update', 'EMAIL', 'Account Status Update', 
'<h2>Account Status Update</h2><p>Your account status has been updated to: <strong>{{newStatus}}</strong></p><p>If you have any questions, please contact the administration.</p><p>Date: {{date}}</p>', 
'["newStatus", "date"]', 'administration', 'Account status update email');
