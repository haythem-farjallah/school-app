import * as React from "react";
import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { 
  Class, 
  CreateClassRequest, 
  UpdateClassRequest,
} from "@/types/class";
import { useQueryApi } from "@/hooks/useQueryApi";

const LIST_KEY = "classes";

/* ── 1. Paginated list with filtering ──────────────────────────────────────────────────── */
export function useClasses(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, ...filters } = options;

  console.log("🔍 useClasses - Called with options:", { size, search, filters });

  // Build QueryParams filter format: filter[attributeName]=value1,value2
  const queryParams = React.useMemo(() => {
    const params: Record<string, unknown> = {};

    // Add search if provided
    if (search && search.trim()) {
      params.search = search.trim();
    }

    // Convert filters to QueryParams format
    Object.entries(filters).forEach(([key, val]) => {
      if (val !== null && val !== undefined) {
        if (Array.isArray(val) && val.length > 0) {
          // For array values (from faceted filters)
          const filterKey = `filter[${key}]`;
          if (key === "yearOfStudy") {
            // Convert string array to number array for yearOfStudy
            const numVals = val.map(v => parseInt(v)).filter(v => !isNaN(v));
            if (numVals.length > 0) {
              params[filterKey] = numVals.join(",");
            }
          } else {
            params[filterKey] = val.join(",");
          }
        } else if (typeof val === "string" && val.trim()) {
          // For string values (from text filters)
          const filterKey = `filter[${key}]`;
          if (key === "yearOfStudy" || key === "maxStudents") {
            // Convert string to number for numeric fields
            const numVal = parseInt(val.trim());
            if (!isNaN(numVal)) {
              params[filterKey] = numVal.toString();
            }
          } else {
            params[filterKey] = val.trim();
          }
        } else if (typeof val === "number") {
          // For direct number values
          const filterKey = `filter[${key}]`;
          params[filterKey] = val.toString();
        }
      }
    });

    console.log("🔍 useClasses - QueryParams:", params);
    return params;
  }, [search, filters]);

  const result = usePaginated<Class>(
    "/v1/classes/new",
    LIST_KEY,
    size,
    queryParams,
    page, // external page number
  );

  console.log("🔍 useClasses - Result:", result);
  return result;
}

/* ── 2. Single class ──────────────────────────────────────────────────── */
export function useClass(id?: number) {
  return useQueryApi<Class>(
    ["class", id],
    async () => {
      const response = await http.get(`/v1/classes/${id}`);
      console.log("🔍 useClass - API response:", response);
      
      // The HTTP interceptor unwraps the response, so we might get:
      // 1. Direct class data if backend returns Class directly
      // 2. ApiResponse wrapper with data property
      if (response && typeof response === 'object' && 'data' in response) {
        return response.data as Class;
      }
      
      // If it's already the class object directly
      return response as Class;
    },
    { enabled: !!id },
  );
}

/* ── 3. Create class ──────────────────────────────────────────────────── */
export function useCreateClass() {
  return useMutationApi<Class, CreateClassRequest>(
    async (classData) => {
      const response = await http.post<Class>("/v1/classes", classData);
      return response.data;
    }
  );
}

/* ── 4. Update class ──────────────────────────────────────────────────── */
export function useUpdateClass() {
  return useMutationApi<Class, { id: number; data: UpdateClassRequest }>(
    async ({ id, data }) => {
      const response = await http.put<Class>(`/v1/classes/${id}`, data);
      return response.data;
    }
  );
}

/* ── 5. Delete class ──────────────────────────────────────────────────── */
export function useDeleteClass() {
  return useMutationApi<void, number>(
    async (id) => {
      await http.delete(`/v1/classes/${id}`);
    }
  );
}

/* ── 6. Add courses to class ──────────────────────────────────────────── */
export function useAddCoursesToClass() {
  return useMutationApi<void, { classId: number; courseIds: number[] }>(
    async ({ classId, courseIds }) => {
      await http.patch(`/v1/classes/${classId}/courses`, {
        operation: "ADD",
        ids: courseIds,
      });
    }
  );
}

/* ── 7. Remove courses from class ──────────────────────────────────────── */
export function useRemoveCoursesFromClass() {
  return useMutationApi<void, { classId: number; courseIds: number[] }>(
    async ({ classId, courseIds }) => {
      await http.patch(`/v1/classes/${classId}/courses`, {
        operation: "REMOVE",
        ids: courseIds,
      });
    }
  );
}

/* ── 8. Add student to class ──────────────────────────────────────────── */
export function useAddStudentToClass() {
  return useMutationApi<void, { classId: number; studentId: number }>(
    async ({ classId, studentId }) => {
      await http.post(`/v1/classes/${classId}/students/${studentId}`);
    }
  );
}

/* ── 9. Remove student from class ──────────────────────────────────────── */
export function useRemoveStudentFromClass() {
  return useMutationApi<void, { classId: number; studentId: number }>(
    async ({ classId, studentId }) => {
      await http.delete(`/v1/classes/${classId}/students/${studentId}`);
    }
  );
} 