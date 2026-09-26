import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { 
  Grade, 
  CreateGradeRequest, 
  UpdateGradeRequest,
  GradeFilters,
  StaffGradeReview,
  StudentGradeSheet,
  TeacherAttendance,
  TeacherAttendanceStatistics,
  Semester,
} from "@/types/grade";
import { useQueryApi } from "@/hooks/useQueryApi";

const LIST_KEY = "grades";

/* ── 1. Paginated list with filters ──────────────────────────────────────────────────── */
export function useGrades(
  options: { size?: number } & GradeFilters = {},
) {
  const { size = 10, ...filters } = options;

  return usePaginated<Grade>(
    "/v1/grades",
    LIST_KEY,
    size,
    filters,
  );
}

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

/* ── 4. Create grade ──────────────────────────────────────────────────── */
export function useCreateGrade() {
  return useMutationApi<Grade, CreateGradeRequest>(
    async (gradeData) => {
      const response = await http.post<Grade>("/v1/grades", gradeData);
      return response.data;
    }
  );
}

/* ── 5. Update grade ──────────────────────────────────────────────────── */
export function useUpdateGrade() {
  return useMutationApi<Grade, { id: number; data: UpdateGradeRequest }>(
    async ({ id, data }) => {
      const response = await http.patch<Grade>(`/v1/grades/${id}`, data);
      return response.data;
    }
  );
}

/* ── 6. Delete grade ──────────────────────────────────────────────────── */
export function useDeleteGrade() {
  return useMutationApi<void, number>(
    async (gradeId) => {
      await http.delete(`/v1/grades/${gradeId}`);
    }
  );
}

/* ── 17. Enhanced Grade System Hooks ──────────────────────────────────────────────────── */

// Staff Grade Review
export function useStaffGradeReviews(classId?: number, semester?: Semester) {
  return useQueryApi<StaffGradeReview[]>(
    ["grades", "staff", "reviews", classId, semester],
    () => http.get<StaffGradeReview[], StaffGradeReview[]>(`/v1/grades/staff/reviews`, {
      params: { classId, semester }
    }),
    { enabled: !!(classId && semester) },
  );
}

export function useApproveGrades() {
  return useMutationApi<void, { studentIds: number[]; semester: Semester; approvedBy: string }>(
    async (approvalData) => {
      await http.post("/v1/grades/approve", approvalData);
    }
  );
}

// Student Grade Sheet
export function useStudentGradeSheet(studentId?: number, semester?: Semester) {
  return useQueryApi<StudentGradeSheet>(
    ["grades", "student", studentId, "sheet", semester],
    () => http.get<StudentGradeSheet, StudentGradeSheet>(`/v1/grades/student/${studentId}/sheet`, {
      params: { semester }
    }),
    { enabled: !!(studentId && semester) },
  );
}

export function useExportGradeSheet() {
  return useMutationApi<Blob, { studentId: number; semester: Semester }>(
    async ({ studentId, semester }) => {
      const response = await http.get(`/v1/grades/student/${studentId}/export`, {
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
    () => http.get<TeacherAttendance[], TeacherAttendance[]>("/v1/teacher-attendance", { params: filters }),
  );
}

export function useCreateTeacherAttendance() {
  return useMutationApi<TeacherAttendance, Omit<TeacherAttendance, 'id' | 'createdAt' | 'updatedAt'>>(
    async (attendanceData) => {
      const response = await http.post<TeacherAttendance>("/v1/teacher-attendance", attendanceData);
      return response.data;
    }
  );
}

export function useTeacherAttendanceStatistics(teacherId?: number) {
  return useQueryApi<TeacherAttendanceStatistics>(
    ["teacher-attendance", "statistics", teacherId],
    () => http.get<TeacherAttendanceStatistics, TeacherAttendanceStatistics>(`/v1/teacher-attendance/statistics/${teacherId}`),
    { enabled: !!teacherId },
  );
} 