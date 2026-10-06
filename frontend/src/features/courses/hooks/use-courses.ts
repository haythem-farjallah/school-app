import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { api } from "@/lib/api-client";
import type { Course } from "@/types/course";
import type { ApiResponse } from "@/types/level";
import React from "react";

const LIST_KEY = "courses";

/* ── 1. Paginated list with search and filters ──────────────────────────────────────────────────── */
export function useCourses(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, name, credit, weeklyCapacity, teacherId } = options;

  const queryParams = React.useMemo(() => {
    const params: Record<string, string> = {};
    if (search?.trim()) params.search = search.trim();
    if (typeof name === "string" && name.trim()) params.name_like = name.trim();

    for (const [parameter, value] of [
      ["credit_eq", credit],
      ["weeklyCapacity_eq", weeklyCapacity],
      ["teacher.id_eq", teacherId],
    ] as const) {
      if (typeof value === "string" && value.trim()) {
        params[parameter] = value.trim();
      } else if (typeof value === "number" && Number.isFinite(value)) {
        params[parameter] = String(value);
      }
    }
    return params;
  }, [search, name, credit, weeklyCapacity, teacherId]);

  return usePaginated<Course>("/v1/courses/filter", LIST_KEY, size, queryParams, page);
}

export function useCreateCourse() {
  return useMutationApi<Course, Omit<Course, "id">>(
    async (courseData) => {
      const response = await api.post<ApiResponse<Course>>("/v1/courses", courseData);
      return response.data.data;
    }
  );
}

export function useUpdateCourse() {
  return useMutationApi<Course, Course>(
    async (courseData) => {
      const response = await api.put<ApiResponse<Course>>(`/v1/courses/${courseData.id}`, courseData);
      return response.data.data;
    }
  );
}

export function useDeleteCourse() {
  return useMutationApi<void, number>(
    async (courseId) => {
      await api.delete(`/v1/courses/${courseId}`);
    }
  );
} 