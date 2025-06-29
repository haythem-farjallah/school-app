import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { Student, CreateStudentData } from "@/types/student";
import { useQueryApi } from "@/hooks/useQueryApi";

const LIST_KEY = "students";

/* ── 1. Paginated list ──────────────────────────────────────────────────── */
export function useStudents(
  options: { size?: number } & Record<string, unknown> = {},
) {
  const { size = 10, ...filters } = options;

  return usePaginated<Student>(
    "/admin/student-management",
    LIST_KEY,
    size,
    filters,
  );
}

/* ── 2. Single student ──────────────────────────────────────────────────── */
export function useStudent(id?: number) {
  return useQueryApi<Student>(
    ["student", id],
    // second generic = final resolved type after response interceptor
    () => http.get<Student, Student>(`/admin/student-management/${id}`),
    { enabled: !!id },
  );
}


export function useCreateStudent() {
  return useMutationApi<Student, CreateStudentData>(
    async (studentData) => {
      const response = await http.post<Student>("/admin/student-management", studentData);
      return response.data;
    }
  );
}

export function useUpdateStudent() {
  return useMutationApi<Student, Student>(
    async (studentData) => {
      const response = await http.put<Student>(`/admin/student-management/${studentData.id}`, studentData);
      return response.data;
    }
  );
}

export function useDeleteStudent() {
  return useMutationApi<void, number>(
    async (id) => {
      await http.delete(`/admin/student-management/${id}`);
    }
  );
} 