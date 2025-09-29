import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { Student, CreateStudentData } from "@/types/student";
import { useQueryApi } from "@/hooks/useQueryApi";
import { useQueryClient } from "@tanstack/react-query";
import React from "react";

const LIST_KEY = "students";

/* ── 1. Paginated list with search and filters ──────────────────────────────────────────────────── */
export function useStudents(
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

  const result = usePaginated<Student>(
    "/v1/students",
    LIST_KEY,
    size,
    searchFilters,
    false,
    page, // external page number
  );


  return result;
}

/* ── 2. Single student ──────────────────────────────────────────────────── */
export function useStudent(id?: number) {
  console.log("👤 useStudent - Called with id:", id);

  const result = useQueryApi<Student>(
    ["student", id],
    async () => {
      const response = await http.get<{ status: string; data: Student }>(`/v1/students/${id}`);
      console.log("👤 useStudent - Raw API response:", response);
      // The HTTP interceptor unwraps the axios response, so response is the API response body
      // Extract the actual student data from the wrapped response
      return (response as unknown as { status: string; data: Student }).data;
    },
    { enabled: !!id },
  );

  console.log("👤 useStudent - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message
  });

  return result;
}

/* ── 3. Create student ──────────────────────────────────────────────────── */
export function useCreateStudent() {
  console.log("➕ useCreateStudent - Hook initialized");
  
  return useMutationApi<Student, CreateStudentData>(
    async (studentData) => {
      console.log("➕ useCreateStudent - Creating student:", studentData);
      const response = await http.post<{ status: string; data: Student }>("/v1/students", studentData);
      console.log("➕ useCreateStudent - Response:", response.data);
      return response.data.data;
    }
  );
}

/* ── 4. Update student ──────────────────────────────────────────────────── */
export function useUpdateStudent() {
  console.log("✏️ useUpdateStudent - Hook initialized");
  const queryClient = useQueryClient();
  
  return useMutationApi<Student, CreateStudentData & { id: number }>(
    async (studentData) => {
      console.log("✏️ useUpdateStudent - Updating student:", studentData);
      
      // Extract the id and create the update payload
      const { id, profile, ...restData } = studentData;
      
      // Flatten the profile data for PATCH request since backend expects flat structure
      const updateData = {
        firstName: profile.firstName,
        lastName: profile.lastName,
        email: profile.email,
        telephone: profile.telephone,
        birthday: profile.birthday,
        gender: profile.gender,
        address: profile.address,
        ...restData // gradeLevel, enrollmentYear
      };
      
      console.log("✏️ useUpdateStudent - Flattened data for PATCH:", updateData);
      
      const response = await http.patch<{ status: string; data: Student }>(`/v1/students/${id}`, updateData);
      console.log("✏️ useUpdateStudent - Response:", response.data);
      return response.data.data;
    },
    {
      onSuccess: (data, variables) => {
        console.log("✅ useUpdateStudent - Success, invalidating cache");
        // Invalidate the students list cache to refresh the data
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        // Also invalidate the specific student query if it exists
        queryClient.invalidateQueries({ queryKey: ["student", variables.id] });
        // Update the cache with the new data to prevent unnecessary refetches
        queryClient.setQueryData(["student", variables.id], data);
      }
    }
  );
}

/* ── 5. Delete student ──────────────────────────────────────────────────── */
export function useDeleteStudent() {
  console.log("🗑️ useDeleteStudent - Hook initialized");
  
  return useMutationApi<void, number>(
    async (studentId) => {
      console.log("🗑️ useDeleteStudent - Deleting student:", studentId);
      await http.delete(`/v1/students/${studentId}`);
      console.log("🗑️ useDeleteStudent - Student deleted successfully");
    }
  );
} 