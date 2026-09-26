import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../../lib/api-client';
import { getApiErrorMessage } from '../../../lib/api-error';
import { Announcement, CreateAnnouncementRequest, AnnouncementFilters } from '../../../types/announcement';
import type { ApiResponse, PageDto } from '../../../types/level';
import toast from 'react-hot-toast';

// Get announcements with filters
export function useAnnouncements(filters: AnnouncementFilters = {}) {
  return useQuery({
    queryKey: ['announcements', filters],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filters.page !== undefined) params.append('page', filters.page.toString());
      if (filters.size !== undefined) params.append('size', filters.size.toString());
      if (filters.importance) params.append('importance', filters.importance);
      if (filters.isPublic !== undefined) params.append('isPublic', filters.isPublic.toString());

      console.log('🔍 useAnnouncements - Making API call with params:', params.toString());
      const response = await api.get<ApiResponse<PageDto<Announcement>>>(`/v1/announcements?${params}`);
      return response.data.data;
    },
  });
}

// Get public announcements (for students/parents)
export function usePublicAnnouncements(page = 0, size = 10) {
  return useQuery({
    queryKey: ['announcements', 'public', page, size],
    queryFn: async () => {
      const response = await api.get<ApiResponse<PageDto<Announcement>>>(`/v1/announcements/public?page=${page}&size=${size}`);
      return response.data.data;
    },
  });
}

// Note: Teacher classes are now fetched using the existing useAllTeacherClasses hook
// from @/hooks/useTeacherClasses instead of a custom implementation

// Create announcement
export function useCreateAnnouncement() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: CreateAnnouncementRequest) => {
      const response = await api.post<ApiResponse<Announcement>>('/v1/announcements', data);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
      toast.success('Announcement created successfully!');
    },
    onError: (error: unknown) => {
      toast.error(getApiErrorMessage(error, 'Failed to create announcement'));
    },
  });
}

// Update announcement
export function useUpdateAnnouncement() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: Partial<CreateAnnouncementRequest> }) => {
      const response = await api.put<ApiResponse<Announcement>>(`/v1/announcements/${id}`, data);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
      toast.success('Announcement updated successfully!');
    },
    onError: (error: unknown) => {
      toast.error(getApiErrorMessage(error, 'Failed to update announcement'));
    },
  });
}

// Delete announcement
export function useDeleteAnnouncement() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/v1/announcements/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
      toast.success('Announcement deleted successfully!');
    },
    onError: (error: unknown) => {
      toast.error(getApiErrorMessage(error, 'Failed to delete announcement'));
    },
  });
}
