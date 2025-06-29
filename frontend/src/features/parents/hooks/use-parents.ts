import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { Parent, CreateParentData } from "@/types/parent";
import { useQueryApi } from "@/hooks/useQueryApi";

const LIST_KEY = "parents";

/* ── 1. Paginated list ──────────────────────────────────────────────────── */
export function useParents(
  options: { size?: number } & Record<string, unknown> = {},
) {
  const { size = 10, ...filters } = options;

  return usePaginated<Parent>(
    "/admin/parent-management",
    LIST_KEY,
    size,
    filters,
  );
}

/* ── 2. Single parent ──────────────────────────────────────────────────── */
export function useParent(id: number) {
  return useQueryApi<Parent>(
    ["parent", id],
    async () => {
      const response = await http.get<Parent>(`/admin/parent-management/${id}`);
      return response.data;
    },
    { enabled: !!id },
  );
}

export function useCreateParent() {
  return useMutationApi<Parent, CreateParentData>(
    async (parentData) => {
      const response = await http.post<Parent>("/admin/parent-management", parentData);
      return response.data;
    }
  );
}

export function useUpdateParent() {
  return useMutationApi<Parent, Parent>(
    async (parentData) => {
      const response = await http.put<Parent>(`/admin/parent-management/${parentData.id}`, parentData);
      return response.data;
    }
  );
}

export function useDeleteParent() {
  return useMutationApi<void, number>(
    async (id) => {
      await http.delete(`/admin/parent-management/${id}`);
    }
  );
} 