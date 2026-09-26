import * as React from "react";
import { usePaginated } from "@/hooks/usePaginated";
import { useQueryApi } from "@/hooks/useQueryApi";
import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";
import type { 
  Enrollment, 
  CreateEnrollmentRequest,
  UpdateEnrollmentStatusRequest,
  DropEnrollmentRequest,
  EnrollmentStatus 
} from "@/types/enrollment";

const LIST_KEY = "enrollments";

export function useEnrollments(
  options: { page?: number; size?: number; search?: string; status?: EnrollmentStatus; studentId?: number; classId?: number } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, status, studentId, classId, ...filters } = options;

  console.log("🔍 useEnrollments - Called with options:", { size, search, status, studentId, classId, filters });

  // Build query parameters
  const queryParams = React.useMemo(() => {
    const params: Record<string, unknown> = {};

    // Add search if provided
    if (search && search.trim()) {
      params.search = search.trim();
    }

    // Add status filter
    if (status) {
      params.status = status;
    }

    // Add student filter
    if (studentId) {
      params.studentId = studentId;
    }

    // Add class filter  
    if (classId) {
      params.classId = classId;
    }

    // Convert other filters
    Object.entries(filters).forEach(([key, val]) => {
      if (val !== null && val !== undefined) {
        if (typeof val === "string" && val.trim()) {
          params[key] = val.trim();
        } else if (typeof val === "number" && val > 0) {
          params[key] = val;
        }
      }
    });

    console.log("🔍 useEnrollments - Query params:", params);
    return params;
  }, [search, status, studentId, classId, filters]);

  const result = usePaginated<Enrollment>(
    "/v1/enrollments",
    LIST_KEY,
    size,
    queryParams,
    page, // external page number
  );

  console.log("🔍 useEnrollments - Result:", { 
    isLoading: result.isLoading, 
    data: result.data?.data?.length,
    total: result.data?.totalItems 
  });

  return result;
}

export function useEnrollment(id?: number) {
  return useQueryApi<Enrollment>(
    ["enrollment", id],
    async () => {
      const response = await http.get(`/v1/enrollments/${id}`);
      // Handle potential API response wrapper
      if (response && typeof response === 'object' && 'data' in response) {
        return response.data as Enrollment;
      }
      return response as Enrollment;
    },
    { enabled: !!id }
  );
}

export function useStudentEnrollments(studentId?: number, options?: { page?: number; size?: number }) {
  const { page, size = 10 } = options || {};
  
  const queryParams = React.useMemo(() => {
    const params: Record<string, unknown> = {};
    if (size) params.size = size;
    return params;
  }, [size]);

  return usePaginated<Enrollment>(
    `/v1/enrollments/student/${studentId}`,
    ["enrollments", "student", studentId?.toString()],
    size,
    queryParams,
    page,
  );
}

export function useClassEnrollments(classId?: number, options?: { page?: number; size?: number }) {
  const { page, size = 10 } = options || {};
  
  const queryParams = React.useMemo(() => {
    const params: Record<string, unknown> = {};
    if (size) params.size = size;
    return params;
  }, [size]);

  return usePaginated<Enrollment>(
    `/v1/enrollments/class/${classId}`,
    ["enrollments", "class", classId?.toString()],
    size,
    queryParams,
    page,
  );
}

export function useEnrollStudent() {
  return useMutationApi<Enrollment, CreateEnrollmentRequest>(
    async (data) => {
      // Backend expects only studentId and classId
      const enrollmentData = {
        studentId: data.studentId,
        classId: data.classId,
      };
      
      const response = await http.post("/v1/enrollments/enroll", enrollmentData);
      return response.data;
    }
  );
}

export function useUpdateEnrollmentStatus() {
  return useMutationApi<Enrollment, UpdateEnrollmentStatusRequest & { id: number }>(
    async (data) => {
      const response = await http.put(`/v1/enrollments/${data.id}/status`, {
        status: data.status
      });
      return response.data;
    }
  );
}

export function useDropEnrollment() {
  return useMutationApi<void, DropEnrollmentRequest & { id: number }>(
    async (data) => {
      const response = await http.delete(`/v1/enrollments/${data.id}`, {
        data: { reason: data.reason }
      });
      return response.data;
    }
  );
}
