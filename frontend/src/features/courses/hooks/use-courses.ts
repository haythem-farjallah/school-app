import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { Course } from "@/types/course";
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
    false,
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
      const response = await http.post<Course>("/v1/courses", courseData);
      return response.data;
    }
  );
}

export function useUpdateCourse() {
  return useMutationApi<Course, Course>(
    async (courseData) => {
      const response = await http.put<Course>(`/v1/courses/${courseData.id}`, courseData);
      return response.data;
    }
  );
}

export function useDeleteCourse() {
  return useMutationApi<void, number>(
    async (courseId) => {
      await http.delete(`/v1/courses/${courseId}`);
    }
  );
} 