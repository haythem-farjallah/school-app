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
import { AttendanceStatus, UserType } from "@/types/attendance";

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
    onError: (error: Error) => {
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
    onError: (error: Error) => {
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
    onError: (error: Error) => {
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "class"] 
      });
      toast.success(`Attendance marked successfully`);
    },
    onError: (error: Error) => {
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "class"] 
      });
      toast.success("Attendance copied from previous day");
    },
    onError: (error: Error) => {
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
        // Get teacher's timetable slots for today
        const timetableResponse = await http.get(`/v1/timetables/teacher/${teacherId}`);
        const slots = timetableResponse.data?.data || [];
        
        // Filter slots for today's day of week
        const today = new Date(targetDate);
        const dayOfWeek = today.toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();
        const todaySlots = slots.filter((slot: { dayOfWeek: string }) => slot.dayOfWeek === dayOfWeek);
        
        return todaySlots.map((slot: { id: number }) => ({
          id: slot.id,
          userId: teacherId,
          timetableSlotId: slot.id,
          date: targetDate,
          status: AttendanceStatus.PRESENT,
          userType: UserType.TEACHER,
          remarks: '',
          excuse: ''
        }));
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
        // For now, return empty array - this would need proper implementation
        // based on actual attendance records
        return [];
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
      try {
        // Get teacher's timetable for the week
        const timetableResponse = await http.get(`/v1/timetables/teacher/${teacherId}`);
        const slots = timetableResponse.data?.data || [];
        
        // Group slots by date (simplified implementation)
        const weeklyData: Record<string, Attendance[]> = {};
        const startDate = new Date(startOfWeek);
        
        for (let i = 0; i < 7; i++) {
          const date = new Date(startDate);
          date.setDate(startDate.getDate() + i);
          const dateStr = date.toISOString().split('T')[0];
          const dayOfWeek = date.toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();
          
        const daySlots = slots.filter((slot: { dayOfWeek: string }) => slot.dayOfWeek === dayOfWeek);
        weeklyData[dateStr] = daySlots.map((slot: { id: number }) => ({
            id: slot.id,
            userId: teacherId,
            timetableSlotId: slot.id,
            date: dateStr,
            status: AttendanceStatus.PRESENT,
            userType: UserType.TEACHER,
            remarks: '',
            excuse: ''
          }));
        }
        
        return weeklyData;
      } catch (error) {
        console.error('Error fetching teacher weekly summary:', error);
        return {};
      }
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

// Get students for a class (simple list) - uses enrollment system
export function useStudentsForClass(classId: number) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "class-students", classId],
    queryFn: async (): Promise<Attendance[]> => {
      if (!classId || classId <= 0) {
        console.log('❌ Invalid classId:', classId);
        return [];
      }

      console.log('🚀 SIMPLE CALL: Getting students for class', classId);
      
      try {
        const url = `/v1/attendance/class/${classId}/students-simple`;
        console.log('📞 Calling:', url);
        
        const response = await http.get(url);
        
        console.log('📥 Raw response:', response);
        
        // The HTTP interceptor unwraps res.data, so response IS the unwrapped data
        // Backend sends: {success: true, data: [students]} 
        // HTTP interceptor gives us: {success: true, data: [students]}
        // So we need response.data for the actual students array
        
        let students = [];
        
        if (response && response.data && Array.isArray(response.data)) {
          students = response.data;
          console.log('✅ SUCCESS: Found', students.length, 'students in response.data');
        } else if (Array.isArray(response)) {
          students = response;
          console.log('✅ SUCCESS: Response is direct array with', students.length, 'students');
        } else {
          console.log('⚠️ Unexpected response structure');
          console.log('⚠️ Response type:', typeof response);
          console.log('⚠️ Response:', response);
          students = [];
        }
        
        console.log('👥 FINAL STUDENTS:', students);
        return students;
      } catch (error: any) {
        console.error('❌ API ERROR:', error);
        console.error('❌ Error status:', error?.response?.status);
        console.error('❌ Error data:', error?.response?.data);
        return [];
      }
    },
    enabled: !!classId && classId > 0,
    staleTime: 30 * 1000, // 30 seconds for testing
    retry: false, // Don't retry for easier debugging
  });
}

// Get enrollments for a class (to verify enrollment system)
export function useClassEnrollments(classId: number) {
  return useQuery({
    queryKey: ["enrollments", "class", classId],
    queryFn: async () => {
      try {
        console.log('🔍 Fetching enrollments for class:', classId);
        const response = await http.get(`/v1/enrollments/by-class/${classId}?size=100`);
        const enrollments = response.data?.data?.content || [];
        console.log('✅ Found', enrollments.length, 'enrollments for class', classId);
        return enrollments;
      } catch (error) {
        console.error('❌ Error fetching enrollments for class:', classId, error);
        return [];
      }
    },
    enabled: !!classId && classId > 0,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Debug: Get attendance service debug info for a class
export function useAttendanceDebug(classId: number) {
  return useQuery({
    queryKey: ["attendance-debug", "class", classId],
    queryFn: async () => {
      try {
        console.log('🔧 Fetching attendance debug info for class:', classId);
        const response = await http.get(`/v1/attendance/debug/class/${classId}/enrollments`);
        const debugInfo = response.data?.data || {};
        console.log('🔧 Debug info for class', classId, ':', debugInfo);
        return debugInfo;
      } catch (error) {
        console.error('❌ Error fetching debug info for class:', classId, error);
        return { error: error instanceof Error ? error.message : 'Unknown error' };
      }
    },
    enabled: !!classId && classId > 0,
    staleTime: 30 * 1000, // 30 seconds (short cache for debugging)
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "slot-students"] 
      });
      // Don't show toast here as components handle it
    },
    onError: (error: Error) => {
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
      time,
      course,
      teacherId,
      attendanceList 
    }: { 
      classId: number; 
      date?: string; 
      time?: string;
      course?: string;
      teacherId?: number;
      attendanceList: CreateAttendanceRequest[] 
    }): Promise<Attendance[]> => {
      const targetDate = date || new Date().toISOString().split('T')[0];
      const payload = {
        attendanceList,
        time,
        course,
        teacherId
      };
      const response = await http.post(`/v1/attendance/class/${classId}/mark?date=${targetDate}`, payload);
      return response.data?.data || [];
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "class-students"]
      });
      // Don't show toast here as components handle it
    },
    onError: (error: Error) => {
      console.error('Failed to mark attendance:', error);
    },
  });
}