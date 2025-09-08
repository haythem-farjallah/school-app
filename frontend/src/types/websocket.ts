export interface RealTimeNotificationDto {
  id: string;
  type: string; // ADMIN_FEED, USER_NOTIFICATION, SYSTEM_ALERT, GRADE_NOTIFICATION, etc.
  title: string;
  message: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  timestamp: string;
  performedBy?: string;
  entityType?: string;
  entityId?: number;
  targetRoles?: string[];
  targetUserIds?: number[];
  actionUrl?: string;
  additionalData?: any;
}

export type NotificationSeverity = 'success' | 'error' | 'info' | 'warning';

export interface WebSocketConfig {
  url: string;
  reconnectDelay: number;
  maxReconnectAttempts: number;
}

export interface ConnectionStatus {
  connected: boolean;
  reconnecting: boolean;
  error?: string;
}
