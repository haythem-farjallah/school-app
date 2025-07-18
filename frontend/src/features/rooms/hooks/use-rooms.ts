import * as React from "react";
import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { 
  Room, 
  CreateRoomRequest, 
  UpdateRoomRequest,
} from "@/types/room";
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
    false,
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
      const response = await http.get(`/v1/rooms/${id}`);
      console.log("🔍 useRoom - API response:", response);
      
      // The HTTP interceptor unwraps the response, so we might get:
      // 1. Direct room data if backend returns Room directly
      // 2. ApiResponse wrapper with data property
      if (response && typeof response === 'object' && 'data' in response) {
        return response.data as Room;
      }
      
      // If it's already the room object directly
      return response as Room;
    },
    { enabled: !!id },
  );
}

/* ── 3. Create room ──────────────────────────────────────────────────── */
export function useCreateRoom() {
  return useMutationApi<Room, CreateRoomRequest>(
    async (roomData) => {
      const response = await http.post<Room>("/v1/rooms", roomData);
      return response.data;
    }
  );
}

/* ── 4. Update room ──────────────────────────────────────────────────── */
export function useUpdateRoom() {
  return useMutationApi<Room, { id: number; data: UpdateRoomRequest }>(
    async ({ id, data }) => {
      const response = await http.put<Room>(`/v1/rooms/${id}`, data);
      return response.data;
    }
  );
}

/* ── 5. Delete room ──────────────────────────────────────────────────── */
export function useDeleteRoom() {
  return useMutationApi<void, number>(
    async (id) => {
      await http.delete(`/v1/rooms/${id}`);
    }
  );
} 