import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { Teacher, CreateTeacherData } from "@/types/teacher";
import { useQueryApi } from "@/hooks/useQueryApi";

const LIST_KEY = "teachers";

/* ── 1. Paginated list ──────────────────────────────────────────────────── */
export function useTeachers(
  options: { size?: number } & Record<string, unknown> = {},
) {
  const { size = 10, ...filters } = options;

  return usePaginated<Teacher>(
    "/admin/teachers",
    LIST_KEY,
    size,
    filters,
  );
}

/* ── 2. Single teacher ──────────────────────────────────────────────────── */
export function useTeacher(id?: number) {
  return useQueryApi<Teacher>(
    ["teacher", id],
    () => http.get<Teacher, Teacher>(`/admin/teachers/${id}`),
    { enabled: !!id },
  );
}

export function useCreateTeacher() {
  return useMutationApi<Teacher, CreateTeacherData>(
    async (teacherData) => {
      const response = await http.post<Teacher>("/admin/teachers", teacherData);
      return response.data;
    }
  );
}

export function useUpdateTeacher() {
  return useMutationApi<Teacher, Teacher>(
    async (teacherData) => {
      const response = await http.put<Teacher>(`/admin/teachers/${teacherData.id}`, teacherData);
      return response.data;
    }
  );
}

export function useDeleteTeacher() {
  return useMutationApi<void, number>(
    async (teacherId) => {
      await http.delete(`/admin/teachers/${teacherId}`);
    }
  );
} 