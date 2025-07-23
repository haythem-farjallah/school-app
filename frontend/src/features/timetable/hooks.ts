import { useQuery } from '@tanstack/react-query';
import { http } from '../../lib/http';
import { Period } from '../../types/period';
import { Timetable } from '../../types/timetable';

export function usePeriods() {
  return useQuery({
    queryKey: ['periods'],
    queryFn: async () => {
      const res = await http.get<{ status: string; data: Period[] }>('/v1/periods');
      return res.data;
    },
  });
}

export function useTimetable(classId: number) {
  return useQuery({
    queryKey: ['timetable', classId],
    queryFn: async () => {
      const res = await http.get<{ status: string; data: Timetable }>(`/v1/timetables/class/${classId}`);
      return res.data;
    },
    enabled: !!classId,
  });
} 