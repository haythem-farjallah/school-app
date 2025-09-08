import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { http } from "@/lib/http";
import toast from "react-hot-toast";
import type { 
  Attendance, 
  CreateAttendanceRequest, 
  BulkAttendanceRequest,
  AttendanceStatistics,
  AttendanceFilters,
  AttendanceResponse,
  ClassAttendanceSummary
} from "@/types/attendance";

const ATTENDANCE_KEY = "attendance";

// Get attendance records with filters
export function useAttendance(filters: AttendanceFilters = {}) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, filters],
    queryFn: async (): Promise<AttendanceResponse> => {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await http.get(`/v1/attendance?${params.toString()}`);
      return response.data;
    },
  });
}

// Get paginated attendance records (like useGrades)
export function useAttendanceRecords(filters: AttendanceFilters = {}) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "records", filters],
    queryFn: async (): Promise<{
      data: Attendance[];
      totalItems: number;
      totalPages: number;
      currentPage: number;
    }> => {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await http.get(`/v1/attendance/filter?${params.toString()}`);
      return response.data;
    },
  });
}

// Get class attendance for a specific date
export function useClassAttendance(classId: number, date: string) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "class", classId, date],
    queryFn: async (): Promise<Attendance[]> => {
      const response = await http.get(`/v1/attendance/class/${classId}?date=${date}`);
      return response.data;
    },
    enabled: !!classId && !!date,
  });
}

// Get class attendance for a date range (for weekly/monthly views)
export function useClassAttendanceRange(
  classId: number, 
  startDate: string, 
  endDate: string
) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "class-range", classId, startDate, endDate],
    queryFn: async (): Promise<Attendance[]> => {
      const response = await http.get(
        `/v1/attendance/class/${classId}/range?startDate=${startDate}&endDate=${endDate}`
      );
      return response.data;
    },
    enabled: !!classId && !!startDate && !!endDate,
  });
}

// Get attendance statistics for a user
export function useUserAttendanceStatistics(
  userId: number, 
  startDate: string, 
  endDate: string
) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "user-statistics", userId, startDate, endDate],
    queryFn: async (): Promise<AttendanceStatistics> => {
      const response = await http.get(
        `/v1/attendance/statistics/${userId}?startDate=${startDate}&endDate=${endDate}`
      );
      return response.data;
    },
    enabled: !!userId && !!startDate && !!endDate,
  });
}

// Get attendance statistics for table
export function useAttendanceStatistics(filters: { startDate?: string; endDate?: string } = {}) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "table-statistics", filters],
    queryFn: async (): Promise<AttendanceStatistics> => {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await http.get(`/v1/attendance/statistics?${params.toString()}`);
      return response.data;
    },
  });
}

// Get class attendance summary for a date range
export function useClassAttendanceSummary(
  classId: number,
  startDate: string,
  endDate: string
) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "class-summary", classId, startDate, endDate],
    queryFn: async (): Promise<ClassAttendanceSummary[]> => {
      const response = await http.get(
        `/v1/attendance/class/${classId}/summary?startDate=${startDate}&endDate=${endDate}`
      );
      return response.data;
    },
    enabled: !!classId && !!startDate && !!endDate,
  });
}

// Create single attendance record
export function useCreateAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: CreateAttendanceRequest): Promise<Attendance> => {
      const response = await http.post("/v1/attendance", data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to record attendance:', error);
    },
  });
}

// Update attendance record
export function useUpdateAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      id, 
      data 
    }: { 
      id: number; 
      data: Partial<CreateAttendanceRequest> 
    }): Promise<Attendance> => {
      const response = await http.patch(`/v1/attendance/${id}`, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to update attendance:', error);
    },
  });
}

// Delete attendance record
export function useDeleteAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      await http.delete(`/v1/attendance/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to delete attendance:', error);
    },
  });
}

// Bulk mark attendance for a class
export function useBulkMarkAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: BulkAttendanceRequest): Promise<void> => {
      await http.post("/v1/attendance/bulk", data);
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "class", variables.classId] 
      });
      toast.success(`Attendance marked for ${variables.attendances.length} students`);
    },
    onError: (error: any) => {
      toast.error(`Failed to mark attendance: ${error.message}`);
    },
  });
}

// Copy attendance from previous day
export function useCopyPreviousAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      classId, 
      fromDate, 
      toDate 
    }: { 
      classId: number; 
      fromDate: string; 
      toDate: string; 
    }): Promise<void> => {
      await http.post("/v1/attendance/copy", {
        classId,
        fromDate,
        toDate,
      });
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "class", variables.classId] 
      });
      toast.success("Attendance copied from previous day");
    },
    onError: (error: any) => {
      toast.error(`Failed to copy attendance: ${error.message}`);
    },
  });
}

// Teacher-specific attendance hooks

// Get teacher's today schedule with attendance status
export function useTeacherTodaySchedule(teacherId: number, date?: string) {
  const targetDate = date || new Date().toISOString().split('T')[0];
  
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "teacher-today", teacherId, targetDate],
    queryFn: async (): Promise<Attendance[]> => {
      try {
        const response = await http.get(`/v1/attendance/teacher/${teacherId}/today?date=${targetDate}`);
        return response.data?.data || [];
      } catch (error) {
        console.error('Error fetching teacher today schedule:', error);
        return [];
      }
    },
    enabled: !!teacherId,
  });
}

// Get absent students for teacher today
export function useTeacherAbsentStudents(teacherId: number, date?: string) {
  const targetDate = date || new Date().toISOString().split('T')[0];
  
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "teacher-absent", teacherId, targetDate],
    queryFn: async (): Promise<Attendance[]> => {
      try {
        const response = await http.get(`/v1/attendance/teacher/${teacherId}/absent-students?date=${targetDate}`);
        return response.data?.data || [];
      } catch (error) {
        console.error('Error fetching teacher absent students:', error);
        return [];
      }
    },
    enabled: !!teacherId,
  });
}

// Get teacher's weekly attendance summary
export function useTeacherWeeklySummary(teacherId: number, startOfWeek: string) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "teacher-weekly", teacherId, startOfWeek],
    queryFn: async (): Promise<Record<string, Attendance[]>> => {
      const response = await http.get(`/v1/attendance/teacher/${teacherId}/weekly-summary?startOfWeek=${startOfWeek}`);
      return response.data?.data || {};
    },
    enabled: !!teacherId && !!startOfWeek,
  });
}

// Get students for a timetable slot with attendance status
export function useStudentsForSlot(slotId: number, date?: string) {
  const targetDate = date || new Date().toISOString().split('T')[0];
  
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "slot-students", slotId, targetDate],
    queryFn: async (): Promise<Attendance[]> => {
      try {
        const response = await http.get(`/v1/attendance/slot/${slotId}/students?date=${targetDate}`);
        return response.data?.data || [];
      } catch (error) {
        console.error('Error fetching students for slot:', error);
        return [];
      }
    },
    enabled: !!slotId,
  });
}

// Get students for a class (simple list)
export function useStudentsForClass(classId: number) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "class-students", classId],
    queryFn: async (): Promise<Attendance[]> => {
      try {
        const response = await http.get(`/v1/attendance/class/${classId}/students-simple`);
        return response.data?.data || [];
      } catch (error) {
        console.error('Error fetching students for class:', classId, error);
        throw error; // Let React Query handle the error
      }
    },
    enabled: !!classId && classId > 0,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Get teacher attendance class view (similar to grade system)
export function useTeacherAttendanceClass(teacherId: number, classId: number, courseId: number) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "teacher-class", teacherId, classId, courseId],
    queryFn: async () => {
      try {
        const response = await http.get(`/v1/attendance/teacher/${teacherId}/class/${classId}/course/${courseId}`);
        return response.data?.data;
      } catch (error) {
        console.error('Error fetching teacher attendance class:', error);
        throw error;
      }
    },
    enabled: !!teacherId && !!classId && !!courseId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Check if teacher can mark attendance for a specific slot
export function useCanTeacherMarkAttendance(teacherId: number, slotId: number, date?: string) {
  const targetDate = date || new Date().toISOString().split('T')[0];
  
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "can-mark", teacherId, slotId, targetDate],
    queryFn: async (): Promise<boolean> => {
      const response = await http.get(`/v1/attendance/teacher/${teacherId}/can-mark/${slotId}?date=${targetDate}`);
      return response.data?.data || false;
    },
    enabled: !!teacherId && !!slotId,
  });
}

// Mark attendance for all students in a timetable slot
export function useMarkAttendanceForSlot() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      slotId, 
      date, 
      attendanceList 
    }: { 
      slotId: number; 
      date?: string; 
      attendanceList: CreateAttendanceRequest[] 
    }): Promise<Attendance[]> => {
      const targetDate = date || new Date().toISOString().split('T')[0];
      const response = await http.post(`/v1/attendance/slot/${slotId}/mark?date=${targetDate}`, attendanceList);
      return response.data?.data || [];
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "slot-students", variables.slotId] 
      });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to mark attendance:', error);
    },
  });
}

// Mark attendance for all students in a class (for virtual slots)
export function useMarkAttendanceForClass() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      classId, 
      date, 
      attendanceList 
    }: { 
      classId: number; 
      date?: string; 
      attendanceList: CreateAttendanceRequest[] 
    }): Promise<Attendance[]> => {
      const targetDate = date || new Date().toISOString().split('T')[0];
      const response = await http.post(`/v1/attendance/class/${classId}/mark?date=${targetDate}`, attendanceList);
      return response.data?.data || [];
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "class-students", variables.classId] 
      });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to mark attendance:', error);
    },
  });
}