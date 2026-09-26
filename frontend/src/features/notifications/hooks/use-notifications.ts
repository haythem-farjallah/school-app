import { useQueryClient } from '@tanstack/react-query';
import { useQueryApi, useMutationApi } from '@/hooks/useMutationApi';
import { api } from '@/lib/api-client';
import type { Notification } from '@/types/notification';
import type { ApiResponse, PageDto } from '@/types/level';

// =====================================================
// NOTIFICATION QUERIES
// =====================================================

export function useNotifications(userId?: number, filters?: {
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
      const response = await api.get<ApiResponse<PageDto<Notification>>>('/v1/notifications', {
        params: {
          page: filters?.page || 0,
          size: filters?.size || 10,
          readStatus: filters?.status === 'unread' ? false : undefined,
        },
      });
      return response.data.data.content;
    }
  );
}

export function useUnreadNotifications(userId?: number) {
  return useQueryApi<Notification[]>(
    ['notifications', 'unread', userId],
    async () => {
      const response = await api.get<ApiResponse<PageDto<Notification>>>('/v1/notifications', {
        params: { 
          page: 0,
          size: 50,
          readStatus: false 
        },
      });
      return response.data.data.content;
    },
    {
      refetchInterval: 30000, // Refetch every 30 seconds
    }
  );
}

// =====================================================
// NOTIFICATION MUTATIONS
// =====================================================

export function useMarkAsRead() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { notificationId: number }>(
    async (data) => {
      await api.patch(`/v1/notifications/${data.notificationId}/read`);
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
  
  return useMutationApi<void, { notificationId: number }>(
    async (data) => {
      await api.delete(`/v1/notifications/${data.notificationId}`);
    },
    {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['notifications'] });
      },
    }
  );
}
