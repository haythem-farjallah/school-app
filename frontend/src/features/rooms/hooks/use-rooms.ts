import * as React from "react";
import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { api } from "@/lib/api-client";
import type { 
  Room, 
  CreateRoomRequest, 
  UpdateRoomRequest,
} from "@/types/room";
import type { ApiResponse } from "@/types/level";
import { useQueryApi } from "@/hooks/useQueryApi";

const LIST_KEY = "rooms";

/* ── 1. Paginated list with filtering ──────────────────────────────────────────────────── */
export function useRooms(
  options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {},
) {
  const { page, size = 10, search, ...filters } = options;

  console.log("🔍 useRooms - Called with options:", { size, search, filters });

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      name: "name",
      capacity: "minCapacity",
      roomType: "roomType",
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

  const result = usePaginated<Room>(
    "/v1/rooms",
    LIST_KEY,
    size,
    searchFilters,
    page, // external page number
  );

  console.log("🔍 useRooms - Result:", {
    data: result.data,
    isLoading: result.isLoading,
    error: result.error?.message,
    totalItems: result.data?.totalItems,
    totalPages: result.data?.totalPages
  });

  return result;
}

/* ── 2. Single room ──────────────────────────────────────────────────── */
export function useRoom(id?: number) {
  return useQueryApi<Room>(
    ["room", id],
    async () => {
      const response = await api.get<ApiResponse<Room>>(`/v1/rooms/${id}`);
      return response.data.data;
    },
    { enabled: !!id },
  );
}

/* ── 3. Create room ──────────────────────────────────────────────────── */
export function useCreateRoom() {
  return useMutationApi<Room, CreateRoomRequest>(
    async (roomData) => {
      const response = await api.post<ApiResponse<Room>>("/v1/rooms", roomData);
      return response.data.data;
    }
  );
}

/* ── 4. Update room ──────────────────────────────────────────────────── */
export function useUpdateRoom() {
  return useMutationApi<Room, { id: number; data: UpdateRoomRequest }>(
    async ({ id, data }) => {
      const response = await api.put<ApiResponse<Room>>(`/v1/rooms/${id}`, data);
      return response.data.data;
    }
  );
}

/* ── 5. Delete room ──────────────────────────────────────────────────── */
export function useDeleteRoom() {
  return useMutationApi<void, number>(
    async (id) => {
      await api.delete(`/v1/rooms/${id}`);
    }
  );
} 