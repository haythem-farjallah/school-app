import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { Teacher, CreateTeacherData, UpdateTeacherData } from "@/types/teacher";
import { useQueryApi } from "@/hooks/useQueryApi";
import { useQueryClient } from "@tanstack/react-query";
import React from "react";

const LIST_KEY = "teachers";

/* ── 1. Paginated list with search and filters ──────────────────────────────────────────────────── */
export function useTeachers(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, ...filters } = options;


  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      firstName: "firstNameLike",
      lastName: "lastNameLike", 
      email: "emailLike",
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

  const result = usePaginated<Teacher>(
    "/admin/teachers",
    LIST_KEY,
    size,
    searchFilters,
    false,
    page, // external page number
  );


  return result;
}

/* ── 2. Single teacher ──────────────────────────────────────────────────── */
export function useTeacher(id?: number) {
  console.log("👤 useTeacher - Called with id:", id);

  const result = useQueryApi<Teacher>(
    ["teacher", id],
    async () => {
      const response = await http.get<{ status: string; data: Teacher }>(`/admin/teachers/${id}`);
      console.log("👤 useTeacher - Raw API response:", response);
      // The HTTP interceptor unwraps the axios response, so response is the API response body
      // Extract the actual teacher data from the wrapped response
      return (response as unknown as { status: string; data: Teacher }).data;
    },
    { enabled: !!id },
  );

  console.log("👤 useTeacher - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message
  });

  return result;
}

/* ── 3. Create teacher ──────────────────────────────────────────────────── */
export function useCreateTeacher() {
  console.log("➕ useCreateTeacher - Hook initialized");
  
  return useMutationApi<Teacher, CreateTeacherData>(
    async (teacherData) => {
      console.log("➕ useCreateTeacher - Creating teacher:", teacherData);
      const response = await http.post<{ status: string; data: Teacher }>("/admin/teachers", teacherData);
      console.log("➕ useCreateTeacher - Response:", response.data);
      return (response as unknown as { status: string; data: Teacher }).data;
    }
  );
}

/* ── 4. Update teacher ──────────────────────────────────────────────────── */
export function useUpdateTeacher() {
  console.log("✏️ useUpdateTeacher - Hook initialized");
  const queryClient = useQueryClient();
  
  return useMutationApi<Teacher, UpdateTeacherData & { id: number }>(
    async (teacherData) => {
      console.log("✏️ useUpdateTeacher - Updating teacher:", teacherData);
      
      // Extract the id and create the update payload
      const { id, ...updateData } = teacherData;
      
      const response = await http.patch<{ status: string; data: Teacher }>(`/admin/teachers/${id}`, updateData);
      console.log("✏️ useUpdateTeacher - Response:", response.data);
      return response.data.data;
    },
    {
      onSuccess: (data, variables) => {
        console.log("✅ useUpdateTeacher - Success, invalidating cache");
        // Invalidate the teachers list cache to refresh the data
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        // Also invalidate the specific teacher query if it exists
        queryClient.invalidateQueries({ queryKey: ["teacher", variables.id] });
        // Update the cache with the new data to prevent unnecessary refetches
        queryClient.setQueryData(["teacher", variables.id], data);
      }
    }
  );
}

/* ── 5. Delete teacher ──────────────────────────────────────────────────── */
export function useDeleteTeacher() {
  console.log("🗑️ useDeleteTeacher - Hook initialized");
  
  return useMutationApi<void, number>(
    async (teacherId) => {
      console.log("🗑️ useDeleteTeacher - Deleting teacher:", teacherId);
      await http.delete(`/admin/teachers/${teacherId}`);
      console.log("🗑️ useDeleteTeacher - Teacher deleted successfully");
    }
  );
} 