import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { api } from "@/lib/api-client";
import type { Parent, CreateParentData, UpdateParentData } from "@/types/parent";
import type { ApiResponse } from "@/types/level";
import { useQueryApi } from "@/hooks/useQueryApi";
import { useQueryClient } from "@tanstack/react-query";
import React from "react";

const LIST_KEY = "parents";

/* ── 1. Paginated list with search and filters ──────────────────────────────────────────────────── */
export function useParents(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, ...filters } = options;

  console.log("🔍 useParents - Called with options:", { size, search, filters });

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      firstName: "firstNameLike",
      lastName: "lastNameLike", 
      email: "emailLike",
      telephone: "telephoneLike",
      preferredContactMethod: "preferredContactMethodLike",
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

  const result = usePaginated<Parent>(
    "/admin/parent-management",
    LIST_KEY,
    size,
    searchFilters,
    page, // external page number
  );

  console.log("🔍 useParents - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message,
    totalElements: result.data?.totalItems,
    totalPages: result.data?.totalPages
  });

  return result;
}

/* ── 2. Single parent ──────────────────────────────────────────────────── */
export function useParent(id?: number) {
  console.log("👤 useParent - Called with id:", id);

  const result = useQueryApi<Parent>(
    ["parent", id],
    async () => {
      const response = await api.get<ApiResponse<Parent>>(`/admin/parent-management/${id}`);
      return response.data.data;
    },
    { enabled: !!id },
  );

  console.log("👤 useParent - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message
  });

  return result;
}

/* ── 3. Create parent ──────────────────────────────────────────────────── */
export function useCreateParent() {
  console.log("➕ useCreateParent - Hook initialized");
  
  return useMutationApi<Parent, CreateParentData>(
    async (parentData) => {
      console.log("➕ useCreateParent - Creating parent:", parentData);
      const response = await api.post<ApiResponse<Parent>>("/admin/parent-management", parentData);
      return response.data.data;
    }
  );
}

/* ── 4. Update parent ──────────────────────────────────────────────────── */
export function useUpdateParent() {
  console.log("✏️ useUpdateParent - Hook initialized");
  const queryClient = useQueryClient();
  
  return useMutationApi<Parent, UpdateParentData & { id: number }>(
    async (parentData) => {
      console.log("✏️ useUpdateParent - Updating parent:", parentData);
      
      // Extract the id and create the update payload
      const { id, ...updateData } = parentData;
      
      const response = await api.patch<ApiResponse<Parent>>(`/admin/parent-management/${id}`, updateData);
      return response.data.data;
    },
    {
      onSuccess: (data, variables) => {
        console.log("✅ useUpdateParent - Success, invalidating cache");
        // Invalidate the parents list cache to refresh the data
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        // Also invalidate the specific parent query if it exists
        queryClient.invalidateQueries({ queryKey: ["parent", variables.id] });
        // Update the cache with the new data to prevent unnecessary refetches
        queryClient.setQueryData(["parent", variables.id], data);
      }
    }
  );
}

/* ── 5. Delete parent ──────────────────────────────────────────────────── */
export function useDeleteParent() {
  console.log("🗑️ useDeleteParent - Hook initialized");
  
  return useMutationApi<void, number>(
    async (parentId) => {
      console.log("🗑️ useDeleteParent - Deleting parent:", parentId);
      await api.delete(`/admin/parent-management/${parentId}`);
      console.log("🗑️ useDeleteParent - Parent deleted successfully");
    }
  );
} 