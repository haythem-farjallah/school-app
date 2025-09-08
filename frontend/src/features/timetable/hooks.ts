import { useQuery } from '@tanstack/react-query';
import { http } from '../../lib/http';
import { Period } from '../../types/period';
import { Timetable } from '../../types/timetable';

export function usePeriods() {
  return useQuery({
    queryKey: ['periods'],
    queryFn: async () => {
      const res = await http.get<Period[]>('/v1/periods');
      console.log('API /v1/periods response:', res);
      return res; // res is already the array!
    },
  });
}

export function useTimetable(classId: number) {
  return useQuery({
    queryKey: ['timetable', classId],
    queryFn: async () => {
      try {
        console.log('🔍 Fetching timetable for class', classId);
        const res = await http.get<{ status: string; data: Timetable }>(`/v1/timetables/class/${classId}`);
        console.log('📥 Timetable API response for class', classId, ':', res);
        console.log('📥 Timetable slots count:', res.data?.slots?.length || 0);
        return res.data; // Return the data property from ApiSuccessResponse
      } catch (err: unknown) {
        const error = err as { response?: { status?: number } };
        console.log('❌ Timetable API error for class', classId, ':', error?.response?.status);
        if (error?.response?.status === 404) {
          console.log('ℹ️ No timetable found for class', classId, '- this is normal for new classes');
          return null; // No timetable exists for this class
        }
        throw err;
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
      const res = await http.get<Timetable[]>('/v1/timetables');
      console.log('API /v1/timetables response:', res);
      return res; // res is already the array!
    },
  });
} 