import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { Staff, CreateStaffRequest, UpdateStaffRequest } from "@/types/staff";
import { useQueryApi } from "@/hooks/useQueryApi";
import { useQueryClient } from "@tanstack/react-query";
import React from "react";

const LIST_KEY = "staff";

/* ── 1. Paginated list with search and filters ──────────────────────────────────────────────────── */
export function useStaff(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, ...filters } = options;

  console.log("🔍 useStaff - Called with options:", { size, search, filters });

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      firstName: "firstNameLike",
      lastName: "lastNameLike",
      email: "emailLike",
      staffType: "staffType",
      department: "department",
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

  const result = usePaginated<Staff>(
    "/admin/staff",
    LIST_KEY,
    size,
    searchFilters,
    false,
    page, // external page number
  );

  console.log("🔍 useStaff - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message,
    totalElements: result.data?.totalItems,
    totalPages: result.data?.totalPages
  });

  return result;
}

/* ── 2. Single staff member ──────────────────────────────────────────────── */
export function useStaffById(id?: number) {
  console.log("👤 useStaffById - Called with id:", id);

  const result = useQueryApi<Staff>(
    ["staff", id],
    async () => {
      const response = await http.get<{ status: string; data: Staff }>(`/admin/staff/${id}`);
      console.log("👤 useStaffById - Raw API response:", response);
      return (response as unknown as { status: string; data: Staff }).data;
    },
    { enabled: !!id },
  );

  console.log("👤 useStaffById - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message
  });

  return result;
}

/* ── 3. Create staff member ──────────────────────────────────────────────── */
export function useCreateStaff() {
  console.log("➕ useCreateStaff - Hook initialized");
  
  return useMutationApi<Staff, CreateStaffRequest>(
    async (staffData) => {
      console.log("➕ useCreateStaff - Creating staff:", staffData);
      const response = await http.post<{ status: string; data: Staff }>("/admin/staff", staffData);
      console.log("➕ useCreateStaff - Response:", response.data);
      return response.data.data;
    }
  );
}

/* ── 4. Update staff member ──────────────────────────────────────────────── */
export function useUpdateStaff() {
  console.log("✏️ useUpdateStaff - Hook initialized");
  const queryClient = useQueryClient();
  
  return useMutationApi<Staff, { id: number; data: UpdateStaffRequest }>(
    async ({ id, data }) => {
      console.log("✏️ useUpdateStaff - Updating staff:", { id, data });
      const response = await http.patch<{ status: string; data: Staff }>(`/admin/staff/${id}`, data);
      console.log("✏️ useUpdateStaff - Response:", response.data);
      return response.data.data;
    },
    {
      onSuccess: (data, variables) => {
        console.log("✅ useUpdateStaff - Success, invalidating cache");
        // Invalidate the staff list cache to refresh the data
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        // Also invalidate the specific staff query if it exists
        queryClient.invalidateQueries({ queryKey: ["staff", variables.id] });
        // Update the cache with the new data to prevent unnecessary refetches
        queryClient.setQueryData(["staff", variables.id], data);
      }
    }
  );
}

/* ── 5. Delete staff member ──────────────────────────────────────────────── */
export function useDeleteStaff() {
  console.log("🗑️ useDeleteStaff - Hook initialized");
  
  return useMutationApi<void, number>(
    async (staffId) => {
      console.log("🗑️ useDeleteStaff - Deleting staff:", staffId);
      await http.delete(`/admin/staff/${staffId}`);
      console.log("🗑️ useDeleteStaff - Staff deleted successfully");
    }
  );
} 