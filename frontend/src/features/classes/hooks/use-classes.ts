import * as React from "react";
import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { api } from "@/lib/api-client";
import type { 
  Class, 
  CreateClassRequest, 
  UpdateClassRequest,
} from "@/types/class";
import type { ApiResponse } from "@/types/level";
import { useQueryApi } from "@/hooks/useQueryApi";

const LIST_KEY = "classes";

/* ── 1. Paginated list with filtering ──────────────────────────────────────────────────── */
export function useClasses(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, name, yearOfStudy, maxStudents } = options;

  const queryParams = React.useMemo(() => {
    const params: Record<string, string> = {};
    if (search?.trim()) params.search = search.trim();
    if (typeof name === "string" && name.trim()) params.name_like = name.trim();

    for (const [field, value] of [["yearOfStudy", yearOfStudy], ["maxStudents", maxStudents]] as const) {
      if (Array.isArray(value)) {
        const numbers = value
          .filter((entry) => typeof entry === "number" || (typeof entry === "string" && entry.trim()))
          .map(Number)
          .filter(Number.isFinite);
        if (numbers.length) params[`${field}_in`] = numbers.join(",");
      } else if (typeof value === "number" || (typeof value === "string" && value.trim())) {
        const number = Number(value);
        if (Number.isFinite(number)) params[`${field}_eq`] = String(number);
      }
    }
    return params;
  }, [search, name, yearOfStudy, maxStudents]);

  return usePaginated<Class>("/v1/classes/filter", LIST_KEY, size, queryParams, page);
}

/* ── 2. Single class ──────────────────────────────────────────────────── */
export function useClass(id?: number) {
  return useQueryApi<Class>(
    ["class", id],
    async () => {
      const response = await api.get<ApiResponse<Class>>(`/v1/classes/${id}`);
      return response.data.data;
    },
    { enabled: !!id },
  );
}

/* ── 3. Create class ──────────────────────────────────────────────────── */
export function useCreateClass() {
  return useMutationApi<Class, CreateClassRequest>(
    async (classData) => {
      const response = await api.post<ApiResponse<Class>>("/v1/classes", classData);
      return response.data.data;
    }
  );
}

/* ── 4. Update class ──────────────────────────────────────────────────── */
export function useUpdateClass() {
  return useMutationApi<Class, { id: number; data: UpdateClassRequest }>(
    async ({ id, data }) => {
      const response = await api.put<ApiResponse<Class>>(`/v1/classes/${id}`, data);
      return response.data.data;
    }
  );
}

/* ── 5. Delete class ──────────────────────────────────────────────────── */
export function useDeleteClass() {
  return useMutationApi<void, number>(
    async (id) => {
      await api.delete(`/v1/classes/${id}`);
    }
  );
}
