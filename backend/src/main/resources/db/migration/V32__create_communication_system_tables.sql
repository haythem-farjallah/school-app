-- =====================================================
-- COMMUNICATION SYSTEM TABLES
-- =====================================================

-- =====================================================
-- NOTIFICATION TEMPLATES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL UNIQUE,
    template_type VARCHAR(20) NOT NULL CHECK (template_type IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'ANNOUNCEMENT')),
    subject VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    variables TEXT,
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
CREATE TABLE IF NOT EXISTS notifications (
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
    metadata TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    error_message TEXT,
    external_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    FOREIGN KEY (template_id) REFERENCES notification_templates(id) ON DELETE SET NULL
);

-- =====================================================
-- COMMUNICATION LOGS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS communication_logs (
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
    provider VARCHAR(100),
    cost DECIMAL(10, 4),
    error_code VARCHAR(100),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- =====================================================
-- USER DEVICE TOKENS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS user_device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500) NOT NULL,
    platform VARCHAR(10) CHECK (platform IN ('ANDROID', 'IOS', 'WEB')) NOT NULL,
    device_info TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_device_token UNIQUE (user_id, device_token)
);

-- =====================================================
-- USER NOTIFICATION PREFERENCES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    grade_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    attendance_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    announcement_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    payment_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    system_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    quiet_hours_timezone VARCHAR(50) DEFAULT 'UTC',
    digest_frequency VARCHAR(10) CHECK (digest_frequency IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY')) DEFAULT 'NONE',
    digest_time TIME DEFAULT '09:00:00',
    preferences_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- NOTIFICATION CAMPAIGNS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_campaigns (
    id BIGSERIAL PRIMARY KEY,
    campaign_name VARCHAR(255) NOT NULL,
    campaign_description TEXT,
    campaign_type VARCHAR(20) CHECK (campaign_type IN ('EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'MULTI_CHANNEL')) NOT NULL,
    target_audience VARCHAR(20) CHECK (target_audience IN ('ALL_USERS', 'STUDENTS', 'TEACHERS', 'PARENTS', 'STAFF', 'CUSTOM')) NOT NULL,
    target_criteria TEXT,
    title VARCHAR(500),
    content TEXT,
    template_id BIGINT,
    priority VARCHAR(10) CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')) NOT NULL DEFAULT 'MEDIUM',
    scheduled_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    status VARCHAR(20) CHECK (status IN ('DRAFT', 'SCHEDULED', 'RUNNING', 'COMPLETED', 'CANCELLED', 'FAILED')) NOT NULL DEFAULT 'DRAFT',
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
CREATE TABLE IF NOT EXISTS notification_campaign_recipients (
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
CREATE TABLE IF NOT EXISTS sms_opt_outs (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(50) NOT NULL UNIQUE,
    opted_out_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255),
    user_id BIGINT
);

-- =====================================================
-- EMAIL BOUNCES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS email_bounces (
    id BIGSERIAL PRIMARY KEY,
    email_address VARCHAR(255) NOT NULL,
    bounce_type VARCHAR(10) CHECK (bounce_type IN ('HARD', 'SOFT', 'COMPLAINT')) NOT NULL,
    bounce_reason VARCHAR(500),
    bounced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notification_id BIGINT,
    external_message_id VARCHAR(255)
);

