import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { api } from '../../lib/api-client';
import type { ApiResponse, PageDto } from '../../types/level';
import { Period } from '../../types/period';
import { Timetable } from '../../types/timetable';

export function usePeriods() {
  return useQuery({
    queryKey: ['periods'],
    queryFn: async () => {
      // GET /v1/periods answers the plain list, without the ApiSuccessResponse envelope.
      const response = await api.get<Period[]>('/v1/periods');
      return response.data;
    },
  });
}

export function useTimetable(classId: number) {
  return useQuery({
    queryKey: ['timetable', classId],
    queryFn: async () => {
      try {
        const response = await api.get<ApiResponse<Timetable>>(`/v1/timetables/class/${classId}`);
        return response.data.data;
      } catch (error) {
        // A class without a timetable answers 404; that is an empty timetable, not a failure.
        if (axios.isAxiosError(error) && error.response?.status === 404) {
          return null;
        }
        throw error;
      }
    },
    enabled: !!classId,
    retry: false, // Don't retry on 404
    staleTime: 0, // Always consider data stale to ensure fresh fetch
    gcTime: 0, // Don't cache the data
    refetchOnWindowFocus: true, // Refetch when window regains focus
    refetchOnMount: true // Always refetch on mount
  });
}

export function useTimetables() {
  return useQuery({
    queryKey: ['timetables'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<PageDto<Timetable>>>('/v1/timetables');
      return response.data.data.content;
    },
  });
}
