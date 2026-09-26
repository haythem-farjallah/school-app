import { useMutationApi } from "@/hooks/useMutationApi";
import { api } from "@/lib/api-client";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";

const LIST_KEY = "teachers";

/* ── Bulk Delete Teachers ──────────────────────────────────────────────── */
export function useBulkDeleteTeachers() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, number[]>(
    async (ids) => {
      console.log("🗑️ useBulkDeleteTeachers - Deleting teachers:", ids);
      await api.delete('/admin/teachers/bulk', { data: ids });
    },
    {
      onSuccess: (_, ids) => {
        console.log("✅ useBulkDeleteTeachers - Success, invalidating cache");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Successfully deleted ${ids.length} teacher${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkDeleteTeachers - Error:", error);
        toast.error("Failed to delete teachers");
      }
    }
  );
}

/* ── Bulk Update Teacher Status ────────────────────────────────────────── */
export function useBulkUpdateTeacherStatus() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { ids: number[]; status: string; reason?: string }>(
    async ({ ids, status, reason }) => {
      console.log("✏️ useBulkUpdateTeacherStatus - Updating status:", { ids, status, reason });
      await api.patch('/admin/teachers/bulk/status', { ids, status, reason });
    },
    {
      onSuccess: (_, { ids, status }) => {
        console.log("✅ useBulkUpdateTeacherStatus - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Updated ${ids.length} teacher${ids.length !== 1 ? 's' : ''} status to ${status}`);
      },
      onError: (error) => {
        console.error("❌ useBulkUpdateTeacherStatus - Error:", error);
        toast.error("Failed to update teacher status");
      }
    }
  );
}

/* ── Bulk Email Teachers ───────────────────────────────────────────────── */
export function useBulkEmailTeachers() {
  return useMutationApi<void, { 
    ids: number[]; 
    subject: string; 
    message: string; 
    actionUrl?: string; 
    actionText?: string 
  }>(
    async ({ ids, subject, message, actionUrl, actionText }) => {
      console.log("📧 useBulkEmailTeachers - Sending emails:", { ids, subject });
      await api.post('/admin/teachers/bulk/email', {
        ids,
        subject,
        message,
        actionUrl,
        actionText
      });
    },
    {
      onSuccess: (_, { ids, subject }) => {
        console.log("✅ useBulkEmailTeachers - Success");
        toast.success(`Email "${subject}" sent to ${ids.length} teacher${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkEmailTeachers - Error:", error);
        toast.error("Failed to send emails");
      }
    }
  );
}

/* ── Bulk Export Teachers ──────────────────────────────────────────────── */
export function useBulkExportTeachers() {
  return useMutationApi<{ data: string | ArrayBuffer; filename: string }, { ids?: number[]; format: 'csv' | 'xlsx' }>(
    async ({ ids, format }) => {
      console.log("📊 useBulkExportTeachers - Exporting:", { ids, format });
      
      const endpoint = format === 'csv' ? '/admin/teachers/export/csv' : '/admin/teachers/export/excel';
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
      const filename = filenameMatch ? filenameMatch[1] : `teachers_export_${new Date().toISOString().split('T')[0]}.${format}`;
      
      return {
        data: response.data,
        filename
      };
    },
    {
      onSuccess: ({ data, filename }, { format }) => {
        console.log("✅ useBulkExportTeachers - Export successful, downloading file:", filename);
        
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
        
        toast.success(`Teachers exported successfully as ${format.toUpperCase()}`);
      },
      onError: (error) => {
        console.error("❌ useBulkExportTeachers - Error:", error);
        toast.error("Failed to export teachers");
      }
    }
  );
}
