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
  const { page, size = 10, search, ...filters } = options;

  console.log("🔍 useCourses - Called with options:", { size, search, filters });

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      name: "nameLike",
      credit: "credit",
      weeklyCapacity: "weeklyCapacity", 
      teacherId: "teacherId",
    };

    const params: Record<string, unknown> = {};

    Object.entries(filters).forEach(([key, val]) => {
      if (typeof val === "string" && val.trim()) {
        const backendKey = keyMap[key] ?? key;
        params[backendKey] = val.trim();
      }
    });

    return params;
  }, [filters]);

  // Add search to filters if provided
  const searchFilters = search ? { search, ...apiParams } : apiParams;

  const result = usePaginated<Course>(
    "/v1/courses",
    LIST_KEY,
    size,
    searchFilters,
    page, // external page number
  );

  console.log("🔍 useCourses - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message,
    totalElements: result.data?.totalItems,
    totalPages: result.data?.totalPages
  });

  return result;
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