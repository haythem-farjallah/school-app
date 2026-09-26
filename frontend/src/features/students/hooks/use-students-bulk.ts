import { useMutationApi } from "@/hooks/useMutationApi";
import { api } from "@/lib/api-client";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";

const LIST_KEY = "students";

export interface BulkDeleteStudentsRequest {
  ids: number[];
}

export interface BulkUpdateStudentsRequest {
  ids: number[];
  updates: {
    status?: string;
    gradeLevel?: string;
    // Add other bulk updatable fields
  };
}

/* ── Bulk Delete Students ──────────────────────────────────────────────── */
export function useBulkDeleteStudents() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, number[]>(
    async (ids) => {
      console.log("🗑️ useBulkDeleteStudents - Deleting students:", ids);
      await api.delete("/v1/students/bulk", { data: ids });
    },
    {
      onSuccess: () => {
        console.log("✅ useBulkDeleteStudents - Success, invalidating cache");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success("Students deleted successfully");
      },
      onError: (error) => {
        console.error("❌ useBulkDeleteStudents - Error:", error);
        toast.error("Failed to delete students");
      }
    }
  );
}

/* ── Bulk Update Student Status ────────────────────────────────────────── */
export function useBulkUpdateStudentStatus() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { ids: number[]; status: string; reason?: string }>(
    async ({ ids, status, reason }) => {
      console.log("✏️ useBulkUpdateStudentStatus - Updating status:", { ids, status, reason });
      await api.patch('/v1/students/bulk/status', { ids, status, reason });
    },
    {
      onSuccess: (_, { ids, status }) => {
        console.log("✅ useBulkUpdateStudentStatus - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Updated ${ids.length} student${ids.length !== 1 ? 's' : ''} status to ${status}`);
      },
      onError: (error) => {
        console.error("❌ useBulkUpdateStudentStatus - Error:", error);
        toast.error("Failed to update student status");
      }
    }
  );
}

/* ── Bulk Export Students ──────────────────────────────────────────────── */
export function useBulkExportStudents() {
  return useMutationApi<{ data: string | ArrayBuffer; filename: string }, { ids?: number[]; format: 'csv' | 'xlsx' }>(
    async ({ ids, format }) => {
      console.log("📊 useBulkExportStudents - Exporting:", { ids, format });
      
      const endpoint = format === 'csv' ? '/v1/students/export/csv' : '/v1/students/export/excel';
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
      const filename = filenameMatch ? filenameMatch[1] : `students_export_${new Date().toISOString().split('T')[0]}.${format}`;
      
      return {
        data: response.data,
        filename
      };
    },
    {
      onSuccess: ({ data, filename }, { format }) => {
        console.log("✅ useBulkExportStudents - Export successful, downloading file:", filename);
        
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
        
        toast.success(`Students exported successfully as ${format.toUpperCase()}`);
      },
      onError: (error) => {
        console.error("❌ useBulkExportStudents - Error:", error);
        toast.error("Failed to export students");
      }
    }
  );
}

/* ── Bulk Email Students ───────────────────────────────────────────────── */
export function useBulkEmailStudents() {
  return useMutationApi<void, { 
    ids: number[]; 
    subject: string; 
    message: string; 
    actionUrl?: string; 
    actionText?: string 
  }>(
    async ({ ids, subject, message, actionUrl, actionText }) => {
      console.log("📧 useBulkEmailStudents - Sending emails:", { ids, subject });
      await api.post('/v1/students/bulk/email', {
        ids,
        subject,
        message,
        actionUrl,
        actionText
      });
    },
    {
      onSuccess: (_, { ids, subject }) => {
        console.log("✅ useBulkEmailStudents - Success");
        toast.success(`Email "${subject}" sent to ${ids.length} student${ids.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkEmailStudents - Error:", error);
        toast.error("Failed to send emails");
      }
    }
  );
}
