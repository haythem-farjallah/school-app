import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { 
  Grade, 
  CreateGradeRequest, 
  UpdateGradeRequest,
  GradeFilters,
  GradeStatistics,
  StudentGradeSummary,
  ClassGradeSummary,
  TeacherGradeSummary,
  EnhancedGrade,
  CreateEnhancedGradeRequest,
  BulkGradeEntryRequest,
  TeacherGradeClassView,
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

/* ── 3. Single grade ──────────────────────────────────────────────────── */
export function useGrade(id?: number) {
  return useQueryApi<Grade>(
    ["grade", id],
    () => http.get<Grade, Grade>(`/v1/grades/${id}`),
    { enabled: !!id },
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

/* ── 7. Grade statistics ──────────────────────────────────────────────────── */
export function useGradeStatistics() {
  return useQueryApi<GradeStatistics>(
    ["grades", "statistics"],
    () => http.get<GradeStatistics, GradeStatistics>("/v1/grades/statistics"),
  );
}

/* ── 8. Student grade summary ──────────────────────────────────────────────────── */
export function useStudentGradeSummary(studentId?: number) {
  return useQueryApi<StudentGradeSummary>(
    ["grades", "student", studentId],
    () => http.get<StudentGradeSummary, StudentGradeSummary>(`/v1/grades/student/${studentId}/summary`),
    { enabled: !!studentId },
  );
}

/* ── 9. Class grade summary ──────────────────────────────────────────────────── */
export function useClassGradeSummary(classId?: number) {
  return useQueryApi<ClassGradeSummary>(
    ["grades", "class", classId],
    () => http.get<ClassGradeSummary, ClassGradeSummary>(`/v1/grades/class/${classId}/summary`),
    { enabled: !!classId },
  );
}

/* ── 10. Teacher grade summary ──────────────────────────────────────────────────── */
export function useTeacherGradeSummary(teacherId?: number) {
  return useQueryApi<TeacherGradeSummary>(
    ["grades", "teacher", teacherId],
    () => http.get<TeacherGradeSummary, TeacherGradeSummary>(`/v1/grades/teacher/${teacherId}/summary`),
    { enabled: !!teacherId },
  );
}

/* ── 11. Grades by enrollment ──────────────────────────────────────────────────── */
export function useGradesByEnrollment(enrollmentId?: number) {
  return useQueryApi<Grade[]>(
    ["grades", "enrollment", enrollmentId],
    () => http.get<Grade[], Grade[]>(`/v1/grades/enrollment/${enrollmentId}`),
    { enabled: !!enrollmentId },
  );
}

/* ── 12. Grades by course ──────────────────────────────────────────────────── */
export function useGradesByCourse(courseId?: number) {
  return useQueryApi<Grade[]>(
    ["grades", "course", courseId],
    () => http.get<Grade[], Grade[]>(`/v1/grades/course/${courseId}`),
    { enabled: !!courseId },
  );
}

/* ── 13. Grades by teacher ──────────────────────────────────────────────────── */
export function useGradesByTeacher(teacherId?: number) {
  return useQueryApi<Grade[]>(
    ["grades", "teacher", teacherId],
    () => http.get<Grade[], Grade[]>(`/v1/grades/teacher/${teacherId}`),
    { enabled: !!teacherId },
  );
}

/* ── 14. Grades by student ──────────────────────────────────────────────────── */
export function useGradesByStudent(studentId?: number) {
  return useQueryApi<Grade[]>(
    ["grades", "student", studentId],
    () => http.get<Grade[], Grade[]>(`/v1/grades/student/${studentId}`),
    { enabled: !!studentId },
  );
}

/* ── 15. Grades by class ──────────────────────────────────────────────────── */
export function useGradesByClass(classId?: number) {
  return useQueryApi<Grade[]>(
    ["grades", "class", classId],
    () => http.get<Grade[], Grade[]>(`/v1/grades/class/${classId}`),
    { enabled: !!classId },
  );
}

/* ── 16. Bulk operations ──────────────────────────────────────────────────── */
export function useBulkCreateGrades() {
  return useMutationApi<Grade[], CreateGradeRequest[]>(
    async (gradesData) => {
      const response = await http.post<Grade[]>("/v1/grades/bulk", gradesData);
      return response.data;
    }
  );
}

export function useBulkUpdateGrades() {
  return useMutationApi<Grade[], { ids: number[]; data: UpdateGradeRequest }>(
    async ({ ids, data }) => {
      const response = await http.patch<Grade[]>("/v1/grades/bulk", { ids, ...data });
      return response.data;
    }
  );
}

export function useBulkDeleteGrades() {
  return useMutationApi<void, number[]>(
    async (gradeIds) => {
      await http.delete("/v1/grades/bulk", { data: gradeIds });
    }
  );
}

/* ── 17. Enhanced Grade System Hooks ──────────────────────────────────────────────────── */

// Teacher Grade Management
export function useTeacherGradeClasses(teacherId?: number) {
  return useQueryApi<TeacherGradeClassView[]>(
    ["grades", "teacher", teacherId, "classes"],
    () => http.get<TeacherGradeClassView[], TeacherGradeClassView[]>(`/v1/grades/teacher/${teacherId}/classes`),
    { enabled: !!teacherId },
  );
}

export function useTeacherGradeClass(teacherId?: number, classId?: number, courseId?: number) {
  return useQueryApi<TeacherGradeClassView>(
    ["grades", "teacher", teacherId, "class", classId, "course", courseId],
    () => http.get<TeacherGradeClassView, TeacherGradeClassView>(`/v1/grades/teacher/${teacherId}/class/${classId}/course/${courseId}`),
    { enabled: !!(teacherId && classId && courseId) },
  );
}

export function useCreateEnhancedGrade() {
  return useMutationApi<EnhancedGrade, CreateEnhancedGradeRequest>(
    async (gradeData) => {
      const response = await http.post<EnhancedGrade>("/v1/grades/enhanced", gradeData);
      return response.data;
    }
  );
}

export function useBulkGradeEntry() {
  return useMutationApi<EnhancedGrade[], BulkGradeEntryRequest>(
    async (bulkData) => {
      const response = await http.post<EnhancedGrade[]>("/v1/grades/bulk-entry", bulkData);
      return response.data;
    }
  );
}

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