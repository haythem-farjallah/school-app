import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { 
  TeachingAssignment, 
  CreateTeachingAssignmentData, 
  UpdateTeachingAssignmentData,
  BulkAssignTeacherToCoursesRequest,
  BulkAssignTeachersToClassRequest
} from "@/types/teaching-assignment";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import React from "react";

const LIST_KEY = "teachingAssignments";

/* ── 1. Paginated list with search and filters ──────────────────────────────────────────────────── */
export function useTeachingAssignments(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, ...filters } = options;

  console.log("🔍 useTeachingAssignments - Called with options:", { size, search, filters });

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      teacherFirstName: "teacher.firstName_like",
      teacherLastName: "teacher.lastName_like",
      teacherEmail: "teacher.email_like",
      courseName: "course.name_like",
      courseCode: "course.code_like",
      className: "clazz.name_like",
      weeklyHours: "weeklyHours",
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

  const result = usePaginated<TeachingAssignment>(
    "/admin/teaching-assignments",
    LIST_KEY,
    size,
    searchFilters,
    false,
    page, // external page number
  );

  console.log("🔍 useTeachingAssignments - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message,
    totalElements: result.data?.totalItems,
    totalPages: result.data?.totalPages
  });

  return result;
}

/* ── 2. Single teaching assignment ──────────────────────────────────────────────────── */
export function useTeachingAssignment(id?: number) {
  const queryClient = useQueryClient();

  return useMutationApi<TeachingAssignment, number>(
    async (assignmentId) => {
      const response = await http.get<TeachingAssignment>(`/admin/teaching-assignments/${assignmentId}`);
      return response.data;
    },
    {
      queryKey: [LIST_KEY, id],
      enabled: !!id,
      onSuccess: () => {
        // No toast for get, it's a read operation
      },
      onError: (error) => {
        toast.error(`Failed to fetch teaching assignment: ${error.message}`);
      },
    }
  );
}

/* ── 3. Create teaching assignment ──────────────────────────────────────────────────── */
export function useCreateTeachingAssignment() {
  const queryClient = useQueryClient();

  return useMutationApi<TeachingAssignment, CreateTeachingAssignmentData>(
    async (data) => {
      const response = await http.post<TeachingAssignment>("/admin/teaching-assignments", data);
      return response.data;
    },
    {
      onSuccess: () => {
        toast.success("Teaching assignment created successfully");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
      },
      onError: (error) => {
        toast.error(`Failed to create teaching assignment: ${error.message}`);
      },
    }
  );
}

/* ── 4. Update teaching assignment ──────────────────────────────────────────────────── */
export function useUpdateTeachingAssignment() {
  const queryClient = useQueryClient();

  return useMutationApi<TeachingAssignment, { id: number; data: UpdateTeachingAssignmentData }>(
    async ({ id, data }) => {
      const response = await http.patch<TeachingAssignment>(`/admin/teaching-assignments/${id}`, data);
      return response.data;
    },
    {
      onSuccess: () => {
        toast.success("Teaching assignment updated successfully");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
      },
      onError: (error) => {
        toast.error(`Failed to update teaching assignment: ${error.message}`);
      },
    }
  );
}

/* ── 5. Delete teaching assignment ──────────────────────────────────────────────────── */
export function useDeleteTeachingAssignment() {
  const queryClient = useQueryClient();

  return useMutationApi<void, number>(
    async (id) => {
      await http.delete(`/admin/teaching-assignments/${id}`);
    },
    {
      onSuccess: () => {
        toast.success("Teaching assignment deleted successfully");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
      },
      onError: (error) => {
        toast.error(`Failed to delete teaching assignment: ${error.message}`);
      },
    }
  );
}

/* ── 6. Bulk delete teaching assignments ──────────────────────────────────────────────────── */
export function useBulkDeleteTeachingAssignments() {
  const queryClient = useQueryClient();

  return useMutationApi<void, number[]>(
    async (ids) => {
      await http.delete("/admin/teaching-assignments/bulk", { data: ids });
    },
    {
      onSuccess: () => {
        toast.success("Selected teaching assignments deleted successfully");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
      },
      onError: (error) => {
        toast.error(`Failed to bulk delete teaching assignments: ${error.message}`);
      },
    }
  );
}

/* ── 7. Assign teacher to multiple courses ──────────────────────────────────────────────────── */
export function useAssignTeacherToCourses() {
  const queryClient = useQueryClient();

  return useMutationApi<void, BulkAssignTeacherToCoursesRequest>(
    async (data) => {
      await http.post("/admin/teaching-assignments/assign/teacher-to-courses", data);
    },
    {
      onSuccess: (_, variables) => {
        toast.success(`Teacher assigned to ${variables.courseIds.length} courses successfully`);
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        queryClient.invalidateQueries({ queryKey: ["teachers"] });
      },
      onError: (error) => {
        toast.error(`Failed to assign teacher to courses: ${error.message}`);
      },
    }
  );
}

/* ── 8. Assign multiple teachers to course ──────────────────────────────────────────────────── */
export function useAssignTeachersToClass() {
  const queryClient = useQueryClient();

  return useMutationApi<void, BulkAssignTeachersToClassRequest>(
    async (data) => {
      await http.post("/admin/teaching-assignments/assign/teachers-to-course", data);
    },
    {
      onSuccess: (_, variables) => {
        toast.success(`${variables.teacherIds.length} teachers assigned to course successfully`);
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        queryClient.invalidateQueries({ queryKey: ["teachers"] });
      },
      onError: (error) => {
        toast.error(`Failed to assign teachers to course: ${error.message}`);
      },
    }
  );
}

/* ── 9. Bulk create teaching assignments ──────────────────────────────────────────────────── */
export function useBulkCreateTeachingAssignments() {
  const queryClient = useQueryClient();

  return useMutationApi<void, CreateTeachingAssignmentData[]>(
    async (assignments) => {
      await http.post("/admin/teaching-assignments/bulk/create", assignments);
    },
    {
      onSuccess: (_, variables) => {
        toast.success(`${variables.length} teaching assignments created successfully`);
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        queryClient.invalidateQueries({ queryKey: ["teachers"] });
      },
      onError: (error) => {
        toast.error(`Failed to bulk create teaching assignments: ${error.message}`);
      },
    }
  );
}

/* ── 10. Get assignments by teacher ──────────────────────────────────────────────────── */
export function useTeachingAssignmentsByTeacher(
  teacherId: number,
  options: { page?: number; size?: number } = {}
) {
  const { page = 0, size = 10 } = options;

  return usePaginated<TeachingAssignment>(
    `/admin/teaching-assignments/teacher/${teacherId}`,
    [LIST_KEY, "teacher", teacherId],
    size,
    {},
    false,
    page
  );
}

/* ── 11. Get assignments by course ──────────────────────────────────────────────────── */
export function useTeachingAssignmentsByCourse(
  courseId: number,
  options: { page?: number; size?: number } = {}
) {
  const { page = 0, size = 10 } = options;

  return usePaginated<TeachingAssignment>(
    `/admin/teaching-assignments/course/${courseId}`,
    [LIST_KEY, "course", courseId],
    size,
    {},
    false,
    page
  );
}

/* ── 12. Get assignments by class ──────────────────────────────────────────────────── */
export function useTeachingAssignmentsByClass(
  classId: number,
  options: { page?: number; size?: number } = {}
) {
  const { page = 0, size = 10 } = options;

  return usePaginated<TeachingAssignment>(
    `/admin/teaching-assignments/class/${classId}`,
    [LIST_KEY, "class", classId],
    size,
    {},
    false,
    page
  );
}

