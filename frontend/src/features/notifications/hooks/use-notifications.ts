import { useQueryClient } from '@tanstack/react-query';
import { useQueryApi, useMutationApi } from '@/hooks/useMutationApi';
import { http } from '@/lib/http';
import type { 
  Notification, 
  NotificationPreferences, 
  NotificationAnalytics,
  EmailRequest,
  SMSRequest,
  PushNotificationRequest,

  CommunicationAnalytics
} from '@/types/notification';

// =====================================================
// NOTIFICATION QUERIES
// =====================================================

export function useNotifications(userId?: string, filters?: {
  status?: string;
  type?: string;
  channel?: string;
  priority?: string;
  page?: number;
  size?: number;
}) {
  return useQueryApi<Notification[]>(
    ['notifications', userId, filters],
    async () => {
      const response = await http.get<Notification[]>('/v1/notifications', {
        params: {
          page: filters?.page || 0,
          size: filters?.size || 10,
          readStatus: filters?.status === 'unread' ? false : undefined,
        },
      });
      return response as unknown as Notification[];
    }
  );
}

export function useUnreadNotifications(userId?: string) {
  return useQueryApi<Notification[]>(
    ['notifications', 'unread', userId],
    async () => {
      const response = await http.get<Notification[]>('/v1/notifications', {
        params: { 
          page: 0,
          size: 50,
          readStatus: false 
        },
      });
      return response as unknown as Notification[];
    },
    {
      refetchInterval: 30000, // Refetch every 30 seconds
    }
  );
}

export function useNotificationAnalytics(userId?: string, dateRange?: {
  startDate: string;
  endDate: string;
}) {
  return useQueryApi<NotificationAnalytics>(
    ['notifications', 'analytics', userId, dateRange],
    async () => {
      const response = await http.get<NotificationAnalytics>('/v1/notifications/analytics', {
        params: {
          userId,
          ...dateRange,
        },
      });
      return response as unknown as NotificationAnalytics;
    }
  );
}

export function useNotificationPreferences(userId: string) {
  return useQueryApi<NotificationPreferences>(
    ['notifications', 'preferences', userId],
    async () => {
      const response = await http.get<NotificationPreferences>(`/v1/notifications/preferences/${userId}`);
      return response as unknown as NotificationPreferences;
    }
  );
}

export function useCommunicationAnalytics(dateRange: {
  startDate: string;
  endDate: string;
}) {
  return useQueryApi<CommunicationAnalytics>(
    ['notifications', 'communication-analytics', dateRange],
    async () => {
      const response = await http.get<CommunicationAnalytics>('/v1/notifications/analytics/communication', {
        params: dateRange,
      });
      return response as unknown as CommunicationAnalytics;
    }
  );
}

// =====================================================
// NOTIFICATION MUTATIONS
// =====================================================

export function useMarkAsRead() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { notificationId: string }>(
    async (data) => {
      await http.patch(`/v1/notifications/${data.notificationId}/read`);
    },
    {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['notifications'] });
      },
    }
  );
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { userId: string }>(
    async (data) => {
      await http.patch('/v1/notifications/read-all');
    },
    {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['notifications'] });
      },
    }
  );
}

export function useDeleteNotification() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { notificationId: string }>(
    async (data) => {
      await http.delete(`/v1/notifications/${data.notificationId}`);
    },
    {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['notifications'] });
      },
    }
  );
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { userId: string; preferences: Partial<NotificationPreferences> }>(
    async (data) => {
      await http.put('/notifications/preferences', data);
    },
    {
      onSuccess: (_, variables) => {
        queryClient.invalidateQueries({ 
          queryKey: ['notifications', 'preferences', variables.userId] 
        });
      },
    }
  );
}

// =====================================================
// EMAIL MUTATIONS (Simplified stubs)
// =====================================================

export function useSendEmail() {
  return useMutationApi<any, EmailRequest>(
    async (data) => {
      return await http.post('/notifications/email/send', data);
    }
  );
}

export function useSendTemplatedEmail() {
  return useMutationApi<any, {
    templateName: string;
    recipientEmail: string;
    variables: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/email/send-templated', data);
    }
  );
}

export function useSendBulkEmails() {
  return useMutationApi<any, {
    recipients: Array<{
      email: string;
      name?: string;
      personalizedVariables?: Record<string, any>;
    }>;
    subject: string;
    content: string;
    templateName?: string;
    globalTemplateVariables?: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/email/send-bulk', data);
    }
  );
}

// =====================================================
// SMS MUTATIONS (Simplified stubs)
// =====================================================

export function useSendSMS() {
  return useMutationApi<any, SMSRequest>(
    async (data) => {
      return await http.post('/notifications/sms/send', data);
    }
  );
}

export function useSendTemplatedSMS() {
  return useMutationApi<any, {
    templateName: string;
    recipientPhone: string;
    variables: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/sms/send-templated', data);
    }
  );
}

export function useSendOTP() {
  return useMutationApi<any, {
    recipientPhone: string;
    otp: string;
    expiryMinutes?: number;
  }>(
    async (data) => {
      return await http.post('/notifications/sms/send-otp', data);
    }
  );
}

export function useSendBulkSMS() {
  return useMutationApi<any, {
    recipients: Array<{
      phone: string;
      name?: string;
      personalizedVariables?: Record<string, any>;
    }>;
    message: string;
    templateName?: string;
    globalTemplateVariables?: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/sms/send-bulk', data);
    }
  );
}

// =====================================================
// PUSH NOTIFICATION MUTATIONS (Simplified stubs)
// =====================================================

export function useSendPushNotification() {
  return useMutationApi<any, PushNotificationRequest>(
    async (data) => {
      return await http.post('/notifications/push/send', data);
    }
  );
}

export function useSendTemplatedPushNotification() {
  return useMutationApi<any, {
    templateName: string;
    recipientId: string;
    variables: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/push/send-templated', data);
    }
  );
}

export function useSendBulkPushNotifications() {
  return useMutationApi<any, {
    recipients: Array<{
      recipientId: string;
      name?: string;
      personalizedData?: Record<string, any>;
    }>;
    title: string;
    body: string;
    templateName?: string;
    globalTemplateVariables?: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/push/send-bulk', data);
    }
  );
}

export function useBroadcastPushNotification() {
  return useMutationApi<any, PushNotificationRequest>(
    async (data) => {
      return await http.post('/notifications/push/broadcast', data);
    }
  );
}

export function useRegisterDeviceToken() {
  return useMutationApi<any, {
    userId: string;
    deviceToken: string;
    platform: 'android' | 'ios' | 'web';
  }>(
    async (data) => {
      return await http.post('/notifications/push/register-device', data);
    }
  );
}

// =====================================================
// REAL-TIME NOTIFICATIONS (Simplified stubs)
// =====================================================

export function useSendRealTimeNotification() {
  return useMutationApi<any, {
    userId: string;
    payload: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/realtime/send', data);
    }
  );
}

export function useBroadcastRealTimeNotification() {
  return useMutationApi<any, {
    payload: Record<string, any>;
  }>(
    async (data) => {
      return await http.post('/notifications/realtime/broadcast', data);
    }
  );
}

// =====================================================
// SYSTEM HEALTH & ANALYTICS (Simplified stubs)
// =====================================================

export function useNotificationSystemHealth() {
  return useQueryApi<{
    emailService: string;
    smsService: string;
    pushNotificationService: string;
    webSocketService: string;
    timestamp: string;
  }>(
    ['notifications', 'health'],
    async () => {
      const response = await http.get('/v1/notifications/health');
      return response as unknown as {
        emailService: string;
        smsService: string;
        pushNotificationService: string;
        webSocketService: string;
        timestamp: string;
      };
    },
    {
      refetchInterval: 60000, // Refetch every minute
    }
  );
}

export function useWebSocketConnections() {
  return useQueryApi<{
    activeConnections: number;
    connectedUsers: string[];
  }>(
    ['notifications', 'websocket-connections'],
    async () => {
      const response = await http.get('/v1/notifications/realtime/connections');
      return response as unknown as {
        activeConnections: number;
        connectedUsers: string[];
      };
    },
    {
      refetchInterval: 30000, // Refetch every 30 seconds
    }
  );
}

export function useEmailAnalytics(dateRange: {
  startDate: string;
  endDate: string;
}) {
  return useQueryApi<any>(
    ['notifications', 'email-analytics', dateRange],
    async () => {
      const response = await http.get('/notifications/email/analytics', {
        params: dateRange,
      });
      return response;
    }
  );
}

export function useSMSAnalytics(dateRange: {
  startDate: string;
  endDate: string;
}) {
  return useQueryApi<any>(
    ['notifications', 'sms-analytics', dateRange],
    async () => {
      const response = await http.get('/notifications/sms/analytics', {
        params: dateRange,
      });
      return response;
    }
  );
}

export function usePushNotificationAnalytics(dateRange: {
  startDate: string;
  endDate: string;
}) {
  return useQueryApi<any>(
    ['notifications', 'push-analytics', dateRange],
    async () => {
      const response = await http.get('/notifications/push/analytics', {
        params: dateRange,
      });
      return response;
    }
  );
}