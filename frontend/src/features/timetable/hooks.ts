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
        const res = await http.get<Timetable>(`/v1/timetables/class/${classId}`);
        return res;
      } catch (err: any) {
        if (err?.response?.status === 404) {
          return null; // No timetable exists for this class
        }
        throw err;
      }
    },
    enabled: !!classId,
    retry: false, // Don't retry on 404
  });
} 