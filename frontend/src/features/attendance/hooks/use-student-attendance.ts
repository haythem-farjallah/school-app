import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/hooks/useAuth";
import { api } from "@/lib/api-client";
import { AttendanceStatus, type AttendanceHistoryRecord } from "@/types/attendance";
import type { ApiResponse } from "@/types/level";
import { attendanceMonthBounds } from "../month";

export function useStudentAttendance(month: string) {
  const { user, isAuthenticated } = useAuth();
  const userId = isAuthenticated && user?.role === "STUDENT" ? user.id : undefined;
  return useQuery({
    queryKey: ["attendance", "student-history", userId, month],
    enabled: userId !== undefined,
    retry: false,
    gcTime: 0,
    queryFn: async ({ signal }): Promise<AttendanceHistoryRecord[]> => {
      const { startDate, endDate } = attendanceMonthBounds(month);
      const response = await api.get<ApiResponse<AttendanceHistoryRecord[]>>(`/v1/attendance/user/${userId}`, {
        params: { startDate, endDate },
        signal,
      });
      const records = response.data.data;
      if (!Array.isArray(records) || records.some((record) =>
        record.userId !== userId || record.date < startDate || record.date > endDate ||
        ![...Object.values(AttendanceStatus), "SICK_LEAVE", "MEDICAL_LEAVE", "OTHER"].includes(record.status),
      )) {
        throw new Error("Invalid attendance history response");
      }
      // Keep only overview fields in the query cache; never retain medical notes or excuses here.
      return records.map(({ id, userId, date, status, className, courseName, teacherName, timetableSlotId }) => ({
        id, userId, date, status, className, courseName, teacherName, timetableSlotId,
      })).sort((a, b) => b.date.localeCompare(a.date) || a.id - b.id);
    },
  });
}
