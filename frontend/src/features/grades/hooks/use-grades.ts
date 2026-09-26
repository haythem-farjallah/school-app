import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { api } from "@/lib/api-client";
import type { 
  Grade, 
  GradeFilters,
  StaffGradeReview,
  StudentGradeSheet,
  TeacherAttendance,
  TeacherAttendanceStatistics,
  Semester,
} from "@/types/grade";
import type { ApiResponse } from "@/types/level";
import { useQueryApi } from "@/hooks/useQueryApi";

/* ── 2. Advanced filtering ──────────────────────────────────────────────────── */
export function useGradesFilter(
  filters: GradeFilters & { size?: number } = {},
) {
  const { size = 20, ...filterParams } = filters;

  return usePaginated<Grade>(
    "/v1/grades/filter",
    ["grades", "filter"],
    size,
    filterParams,
  );
}

/* ── 17. Enhanced Grade System Hooks ──────────────────────────────────────────────────── */

// Staff Grade Review
export function useStaffGradeReviews(classId?: number, semester?: Semester) {
  return useQueryApi<StaffGradeReview[]>(
    ["grades", "staff", "reviews", classId, semester],
    async () => {
      const response = await api.get<ApiResponse<StaffGradeReview[]>>(`/v1/grades/staff/reviews`, {
        params: { classId, semester }
      });
      return response.data.data;
    },
    { enabled: !!(classId && semester) },
  );
}

export function useApproveGrades() {
  return useMutationApi<void, { studentIds: number[]; semester: Semester; approvedBy: string }>(
    async (approvalData) => {
      await api.post("/v1/grades/approve", approvalData);
    }
  );
}

// Student Grade Sheet
export function useStudentGradeSheet(studentId?: number, semester?: Semester) {
  return useQueryApi<StudentGradeSheet>(
    ["grades", "student", studentId, "sheet", semester],
    async () => {
      const response = await api.get<ApiResponse<StudentGradeSheet>>(`/v1/grades/student/${studentId}/sheet`, {
        params: { semester }
      });
      return response.data.data;
    },
    { enabled: !!(studentId && semester) },
  );
}

export function useExportGradeSheet() {
  return useMutationApi<Blob, { studentId: number; semester: Semester }>(
    async ({ studentId, semester }) => {
      const response = await api.get<Blob>(`/v1/grades/student/${studentId}/export`, {
        params: { semester },
        responseType: 'blob'
      });
      return response.data;
    }
  );
}

// Teacher Attendance Hooks
export function useTeacherAttendance(filters: { teacherId?: number; startDate?: string; endDate?: string } = {}) {
  return useQueryApi<TeacherAttendance[]>(
    ["teacher-attendance", filters],
    async () => {
      const response = await api.get<ApiResponse<TeacherAttendance[]>>("/v1/teacher-attendance", { params: filters });
      return response.data.data;
    },
  );
}

export function useCreateTeacherAttendance() {
  return useMutationApi<TeacherAttendance, Omit<TeacherAttendance, 'id' | 'createdAt' | 'updatedAt'>>(
    async (attendanceData) => {
      const response = await api.post<ApiResponse<TeacherAttendance>>("/v1/teacher-attendance", attendanceData);
      return response.data.data;
    }
  );
}

export function useTeacherAttendanceStatistics(teacherId?: number) {
  return useQueryApi<TeacherAttendanceStatistics>(
    ["teacher-attendance", "statistics", teacherId],
    async () => {
      const response = await api.get<ApiResponse<TeacherAttendanceStatistics>>(`/v1/teacher-attendance/statistics/${teacherId}`);
      return response.data.data;
    },
    { enabled: !!teacherId },
  );
} 