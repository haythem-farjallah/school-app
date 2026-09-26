import { useMutationApi } from "@/hooks/useMutationApi";
import { api } from "@/lib/api-client";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";

const LIST_KEY = "staff";

/* ── Bulk Delete Staff ──────────────────────────────────────────────── */
export function useBulkDeleteStaff() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, number[]>(
    async (ids) => {
      console.log("🗑️ useBulkDeleteStaff - Deleting staff:", ids);
      await api.delete('/admin/staff/bulk', { data: ids });
    },
    {
      onSuccess: (_, ids) => {
        console.log("✅ useBulkDeleteStaff - Success, invalidating cache");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Successfully deleted ${ids.length} staff member${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkDeleteStaff - Error:", error);
        toast.error("Failed to delete staff members");
      }
    }
  );
}

/* ── Bulk Update Staff Status ────────────────────────────────────────── */
export function useBulkUpdateStaffStatus() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { ids: number[]; status: string; reason?: string }>(
    async ({ ids, status, reason }) => {
      console.log("✏️ useBulkUpdateStaffStatus - Updating status:", { ids, status, reason });
      await api.patch('/admin/staff/bulk/status', { ids, status, reason });
    },
    {
      onSuccess: (_, { ids, status }) => {
        console.log("✅ useBulkUpdateStaffStatus - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Updated ${ids.length} staff member${ids.length !== 1 ? 's' : ''} status to ${status}`);
      },
      onError: (error) => {
        console.error("❌ useBulkUpdateStaffStatus - Error:", error);
        toast.error("Failed to update staff status");
      }
    }
  );
}

/* ── Bulk Update Department ────────────────────────────────────────── */
export function useBulkUpdateDepartment() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { ids: number[]; department: string }>(
    async ({ ids, department }) => {
      console.log("🏢 useBulkUpdateDepartment - Updating department:", { ids, department });
      await Promise.all(
        ids.map(id => 
          api.patch(`/admin/staff/${id}`, { 
            department
          })
        )
      );
    },
    {
      onSuccess: (_, { ids }) => {
        console.log("✅ useBulkUpdateDepartment - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Updated department for ${ids.length} staff member${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkUpdateDepartment - Error:", error);
        toast.error("Failed to update department");
      }
    }
  );
}

/* ── Bulk Update Staff Type ────────────────────────────────────────── */
export function useBulkUpdateStaffType() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { ids: number[]; staffType: string }>(
    async ({ ids, staffType }) => {
      console.log("👔 useBulkUpdateStaffType - Updating staff type:", { ids, staffType });
      await Promise.all(
        ids.map(id => 
          api.patch(`/admin/staff/${id}`, { 
            staffType
          })
        )
      );
    },
    {
      onSuccess: (_, { ids }) => {
        console.log("✅ useBulkUpdateStaffType - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Updated staff type for ${ids.length} member${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkUpdateStaffType - Error:", error);
        toast.error("Failed to update staff type");
      }
    }
  );
}

/* ── Bulk Export Staff ──────────────────────────────────────────────── */
export function useBulkExportStaff() {
  return useMutationApi<{ data: string | ArrayBuffer; filename: string }, { ids?: number[]; format: 'csv' | 'xlsx' }>(
    async ({ ids, format }) => {
      console.log("📊 useBulkExportStaff - Exporting:", { ids, format });
      
      const endpoint = format === 'csv' ? '/admin/staff/export/csv' : '/admin/staff/export/excel';
      const payload = ids && ids.length > 0 ? { ids } : {};
      
      const response = await api.post<string | ArrayBuffer>(endpoint, payload, {
        responseType: format === 'csv' ? 'text' : 'arraybuffer',
        headers: {
          'Accept': format === 'csv' ? 'text/csv' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }
      });
      
      // Extract filename from Content-Disposition header
      const contentDisposition = response.headers['content-disposition'];
      const filenameMatch = contentDisposition?.match(/filename="(.+)"/);
      const filename = filenameMatch ? filenameMatch[1] : `staff_export_${new Date().toISOString().split('T')[0]}.${format}`;
      
      return {
        data: response.data,
        filename
      };
    },
    {
      onSuccess: ({ data, filename }, { format }) => {
        console.log("✅ useBulkExportStaff - Export successful, downloading file:", filename);
        
        // Create and download the file
        let blob: Blob;
        if (format === 'csv') {
          blob = new Blob([data as string], { type: 'text/csv;charset=utf-8;' });
        } else {
          blob = new Blob([data as ArrayBuffer], { 
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
          });
        }
        
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        toast.success(`Staff exported successfully as ${format.toUpperCase()}`);
      },
      onError: (error) => {
        console.error("❌ useBulkExportStaff - Error:", error);
        toast.error("Failed to export staff");
      }
    }
  );
}

/* ── Bulk Email Staff ───────────────────────────────────────────────── */
export function useBulkEmailStaff() {
  return useMutationApi<void, { 
    ids: number[]; 
    subject: string; 
    message: string; 
    actionUrl?: string; 
    actionText?: string 
  }>(
    async ({ ids, subject, message, actionUrl, actionText }) => {
      console.log("📧 useBulkEmailStaff - Sending emails:", { ids, subject });
      await api.post('/admin/staff/bulk/email', {
        ids,
        subject,
        message,
        actionUrl,
        actionText
      });
    },
    {
      onSuccess: (_, { ids, subject }) => {
        console.log("✅ useBulkEmailStaff - Success");
        toast.success(`Email "${subject}" sent to ${ids.length} staff member${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkEmailStaff - Error:", error);
        toast.error("Failed to send emails");
      }
    }
  );
}
