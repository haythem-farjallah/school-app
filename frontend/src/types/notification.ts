export interface Notification {
  id: string;
  title: string;
  body: string;
  type: NotificationType;
  priority: NotificationPriority;
  status: NotificationStatus;
  channel: NotificationChannel;
  recipientId: string;
  recipientType: RecipientType;
  createdAt: string;
  sentAt?: string;
  readAt?: string;
  scheduledAt?: string;
  metadata?: Record<string, any>;
  icon?: string;
  image?: string;
  clickAction?: string;
  data?: Record<string, any>;
}

export enum NotificationType {
  GRADE_PUBLISHED = 'GRADE_PUBLISHED',
  ASSIGNMENT_DUE = 'ASSIGNMENT_DUE',
  ATTENDANCE_ALERT = 'ATTENDANCE_ALERT',
  ANNOUNCEMENT = 'ANNOUNCEMENT',
  SCHEDULE_CHANGE = 'SCHEDULE_CHANGE',
  PAYMENT_REMINDER = 'PAYMENT_REMINDER',
  WELCOME = 'WELCOME',
  PASSWORD_RESET = 'PASSWORD_RESET',
  SYSTEM_ALERT = 'SYSTEM_ALERT',
  CUSTOM = 'CUSTOM'
}

export enum NotificationPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export enum NotificationStatus {
  PENDING = 'PENDING',
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  READ = 'READ',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export enum NotificationChannel {
  EMAIL = 'EMAIL',
  SMS = 'SMS',
  PUSH_NOTIFICATION = 'PUSH_NOTIFICATION',
  IN_APP = 'IN_APP',
  ALL_CHANNELS = 'ALL_CHANNELS'
}

export enum RecipientType {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  PARENT = 'PARENT',
  STAFF = 'STAFF',
  ADMIN = 'ADMIN',
  ALL_USERS = 'ALL_USERS',
  CLASS = 'CLASS',
  COURSE = 'COURSE'
}

export interface NotificationPreferences {
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  inAppEnabled: boolean;
  gradeNotifications: boolean;
  assignmentNotifications: boolean;
  attendanceNotifications: boolean;
  announcementNotifications: boolean;
  scheduleNotifications: boolean;
  paymentNotifications: boolean;
  systemNotifications: boolean;
  quietHoursEnabled: boolean;
  quietHoursStart?: string;
  quietHoursEnd?: string;
  digestFrequency: 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
}

export interface NotificationAnalytics {
  totalNotifications: number;
  unreadCount: number;
  readCount: number;
  statusBreakdown: Record<NotificationStatus, number>;
  channelBreakdown: Record<NotificationChannel, number>;
  typeBreakdown: Record<NotificationType, number>;
  priorityBreakdown: Record<NotificationPriority, number>;
  recentActivity: Notification[];
}

export interface EmailRequest {
  recipientEmail: string;
  subject: string;
  content: string;
  recipientName?: string;
  isHtml?: boolean;
  priority?: NotificationPriority;
  templateName?: string;
  templateVariables?: Record<string, any>;
  scheduledAt?: string;
  enableTracking?: boolean;
}

export interface SMSRequest {
  recipientPhone: string;
  message: string;
  recipientName?: string;
  priority?: NotificationPriority;
  templateName?: string;
  templateVariables?: Record<string, any>;
  scheduledAt?: string;
  smsType?: 'TRANSACTIONAL' | 'PROMOTIONAL' | 'EMERGENCY' | 'REMINDER' | 'NOTIFICATION';
}

export interface PushNotificationRequest {
  recipientId: string;
  title: string;
  body: string;
  icon?: string;
  image?: string;
  sound?: string;
  clickAction?: string;
  priority?: NotificationPriority;
  data?: Record<string, any>;
  templateName?: string;
  templateVariables?: Record<string, any>;
  scheduledAt?: string;
  enableSound?: boolean;
  enableVibration?: boolean;
}

export interface BulkNotificationRequest {
  recipients: Array<{
    recipientId: string;
    recipientType: RecipientType;
    personalizedData?: Record<string, any>;
  }>;
  title: string;
  content: string;
  channel: NotificationChannel;
  priority?: NotificationPriority;
  templateName?: string;
  globalTemplateVariables?: Record<string, any>;
  scheduledAt?: string;
  campaignName?: string;
}

export interface NotificationTemplate {
  id: string;
  templateName: string;
  templateType: 'EMAIL' | 'SMS' | 'PUSH_NOTIFICATION' | 'ANNOUNCEMENT';
  subject: string;
  content: string;
  variables: string[];
  isActive: boolean;
  language: string;
  category: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CommunicationAnalytics {
  totalSent: number;
  totalDelivered: number;
  totalOpened: number;
  totalClicked: number;
  totalFailed: number;
  deliveryRate: number;
  openRate: number;
  clickRate: number;
  failureRate: number;
  costAnalysis: {
    totalCost: number;
    costByChannel: Record<NotificationChannel, number>;
    averageCostPerMessage: number;
  };
  performanceMetrics: {
    averageDeliveryTime: number;
    averageDeliveryTimeByChannel: Record<NotificationChannel, number>;
  };
  engagementMetrics: {
    topPerformingTemplates: Array<{
      templateName: string;
      openRate: number;
      clickRate: number;
    }>;
    userEngagementByType: Record<RecipientType, {
      openRate: number;
      clickRate: number;
    }>;
  };
}
