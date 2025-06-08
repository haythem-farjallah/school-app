import { useQueryApi } from "@/hooks/useQueryApi";
import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";
import type { Course, CoursesResponse } from "@/types/course";

interface UseCoursesParams {
  page?: number;
  size?: number;
  search?: string;
  sortBy?: string;
  sortOrder?: "asc" | "desc";
}

export function useCourses(params: UseCoursesParams = {}) {
  const {
    page = 0,
    size = 10,
    search,
    sortBy,
    sortOrder = "asc",
  } = params;

  const query = useQueryApi<CoursesResponse>(
    ["courses", page, size, search, sortBy, sortOrder],
    async () => {
      const response = await http.get<CoursesResponse>("/v1/courses", {
        params: {
          page,
          size,
          ...(search && { search }),
          ...(sortBy && { sortBy, sortOrder }),
        },
      });
      console.log("🔍 Courses API Response:", response.data);
      return response.data;
    },
    {
      placeholderData: (prev) => prev,
    }
  );

  console.log("📊 Query Data:", query.data);
  console.log("📋 Processed Courses:", query.data?.content || []);
  
  return {
    ...query,
    courses: query.data?.content || [],
    totalElements: query.data?.totalElements || 0,
    totalPages: Math.ceil((query.data?.totalElements || 0) / size),
  };
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