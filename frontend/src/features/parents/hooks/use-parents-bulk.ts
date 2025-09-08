import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import type { ImportResult } from "@/components/data-table/bulk-import-dialog";

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

/* ── Bulk Update Parent Status ────────────────────────────────────────── */
export function useBulkUpdateParentStatus() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { ids: number[]; status: string }>(
    async ({ ids, status }) => {
      console.log("✏️ useBulkUpdateParentStatus - Updating status:", { ids, status });
      await Promise.all(
        ids.map(id => 
          http.patch(`/admin/parent-management/${id}`, { 
            profile: { status }
          })
        )
      );
    },
    {
      onSuccess: (_, { ids, status }) => {
        console.log("✅ useBulkUpdateParentStatus - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Updated ${ids.length} parent${ids.length !== 1 ? 's' : ''} status to ${status}`);
      },
      onError: (error) => {
        console.error("❌ useBulkUpdateParentStatus - Error:", error);
        toast.error("Failed to update parent status");
      }
    }
  );
}

/* ── Bulk Update Contact Method ────────────────────────────────────────── */
export function useBulkUpdateContactMethod() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { ids: number[]; contactMethod: string }>(
    async ({ ids, contactMethod }) => {
      console.log("📞 useBulkUpdateContactMethod - Updating contact method:", { ids, contactMethod });
      await Promise.all(
        ids.map(id => 
          http.patch(`/admin/parent-management/${id}`, { 
            preferredContactMethod: contactMethod
          })
        )
      );
    },
    {
      onSuccess: (_, { ids, contactMethod }) => {
        console.log("✅ useBulkUpdateContactMethod - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Updated contact method for ${ids.length} parent${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkUpdateContactMethod - Error:", error);
        toast.error("Failed to update contact method");
      }
    }
  );
}

/* ── Bulk Export Parents ──────────────────────────────────────────────── */
export function useBulkExportParents() {
  return useMutationApi<Blob, { ids?: number[]; format: 'csv' | 'xlsx' }>(
    async ({ ids, format }) => {
      console.log("📊 useBulkExportParents - Exporting:", { ids, format });
      
      const params = new URLSearchParams();
      if (ids && ids.length > 0) {
        params.append('ids', ids.join(','));
      }
      params.append('format', format);
      
      const response = await http.get(`/admin/parent-management/export?${params.toString()}`, {
        responseType: 'blob'
      });
      
      return response.data;
    },
    {
      onSuccess: (blob, { format }) => {
        // Download the file
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `parents.${format}`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        toast.success("Parents exported successfully");
      },
      onError: (error) => {
        console.error("❌ useBulkExportParents - Error:", error);
        toast.error("Failed to export parents");
      }
    }
  );
}

/* ── Bulk Import Parents ──────────────────────────────────────────────── */
export function useBulkImportParents() {
  const queryClient = useQueryClient();
  
  return useMutationApi<ImportResult[], File>(
    async (file) => {
      console.log("📁 useBulkImportParents - Processing file:", file.name);
      
      // Simulate file processing with progress updates
      const results: ImportResult[] = [];
      const mockData = [
        { firstName: "Robert", lastName: "Miller", email: "robert.miller@gmail.com", phoneNumber: "+1234567893", address: "123 Main St" },
        { firstName: "Lisa", lastName: "Anderson", email: "lisa.anderson@gmail.com", phoneNumber: "+1234567894", address: "456 Oak Ave" },
        { firstName: "Mark", lastName: "Taylor", email: "mark.taylor@gmail.com", phoneNumber: "+1234567895", address: "789 Pine Rd" }
      ];

      for (let i = 0; i < mockData.length; i++) {
        const data = mockData[i];
        const result: ImportResult = {
          id: `parent-${i + 1}`,
          row: i + 1,
          data,
          status: 'pending',
        };
        results.push(result);
      }

      // Simulate processing each row
      for (const result of results) {
        result.status = 'processing';
        await new Promise(resolve => setTimeout(resolve, 800));
        
        try {
          // Simulate API call
          await http.post('/admin/parents', result.data);
          result.status = 'success';
          console.log(`✅ Parent imported: ${result.data.firstName} ${result.data.lastName}`);
        } catch (error) {
          result.status = 'error';
          result.error = 'Failed to create parent record';
          console.error(`❌ Failed to import parent: ${result.data.firstName} ${result.data.lastName}`, error);
        }
      }

      return results;
    },
    {
      onSuccess: (results) => {
        const successful = results.filter(r => r.status === 'success').length;
        const failed = results.filter(r => r.status === 'error').length;
        
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        
        if (failed === 0) {
          toast.success(`Successfully imported ${successful} parents`);
        } else {
          toast.success(`Imported ${successful} parents, ${failed} failed`);
        }
      },
      onError: (error) => {
        console.error("❌ useBulkImportParents - Error:", error);
        toast.error("Failed to import parents");
      }
    }
  );
}
