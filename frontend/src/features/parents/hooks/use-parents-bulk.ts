import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";

const LIST_KEY = "parents";

/* ── Bulk Delete Parents ──────────────────────────────────────────────── */
export function useBulkDeleteParents() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, number[]>(
    async (ids) => {
      console.log("🗑️ useBulkDeleteParents - Deleting parents:", ids);
      // Simulate bulk delete by calling individual delete endpoints
      await Promise.all(ids.map(id => http.delete(`/admin/parent-management/${id}`)));
    },
    {
      onSuccess: () => {
        console.log("✅ useBulkDeleteParents - Success, invalidating cache");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success("Parents deleted successfully");
      },
      onError: (error) => {
        console.error("❌ useBulkDeleteParents - Error:", error);
        toast.error("Failed to delete parents");
      }
    }
  );
}
