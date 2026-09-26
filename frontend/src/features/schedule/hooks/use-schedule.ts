import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api-client';
import type { ApiResponse } from '../../../types/level';
import { TimetableSlot } from '../../../types/timetable';

/**
 * Hook to fetch teacher's schedule with assigned classes and time slots
 * @param teacherId - The ID of the teacher
 * @returns Query result with teacher's timetable slots
 */
export function useTeacherSchedule(teacherId?: number) {
  return useQuery({
    queryKey: ['teacher-schedule', teacherId],
    queryFn: async () => {
      const response = await api.get<ApiResponse<TimetableSlot[]>>(`/v1/timetables/teacher/${teacherId}`);
      return response.data.data;
    },
    enabled: !!teacherId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}
