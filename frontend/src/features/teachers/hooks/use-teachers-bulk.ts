import { useMutationApi } from "@/hooks/useMutationApi";
import { api } from "@/lib/api-client";
import { http } from "@/lib/http";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import type { ImportResult } from "@/components/data-table/bulk-import-dialog";

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

/* ── Bulk Assign Courses to Teachers ───────────────────────────────────── */
export function useBulkAssignCourses() {
  const queryClient = useQueryClient();
  
  return useMutationApi<void, { teacherIds: number[]; courseIds: number[] }>(
    async ({ teacherIds, courseIds }) => {
      console.log("📚 useBulkAssignCourses - Assigning courses:", { teacherIds, courseIds });
      // This would need a backend endpoint for bulk course assignment
      await Promise.all(
        teacherIds.map(teacherId =>
          http.post(`/admin/teachers/${teacherId}/courses`, { courseIds })
        )
      );
    },
    {
      onSuccess: (_, { teacherIds, courseIds }) => {
        console.log("✅ useBulkAssignCourses - Success");
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Assigned ${courseIds.length} course${courseIds.length !== 1 ? 's' : ''} to ${teacherIds.length} teacher${teacherIds.length !== 1 ? 's' : ''}`);
      },
      onError: (error) => {
        console.error("❌ useBulkAssignCourses - Error:", error);
        toast.error("Failed to assign courses");
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

/* ── Bulk Import Teachers ──────────────────────────────────────────────── */
export function useBulkImportTeachers() {
  const queryClient = useQueryClient();
  
  return useMutationApi<ImportResult[], File>(
    async (file) => {
      console.log("📁 useBulkImportTeachers - Processing file:", file.name);
      
      // Simulate file processing with progress updates
      const results: ImportResult[] = [];
      const mockData = [
        { firstName: "Sarah", lastName: "Wilson", email: "sarah.wilson@school.edu", department: "Mathematics" },
        { firstName: "David", lastName: "Brown", email: "david.brown@school.edu", department: "Science" },
        { firstName: "Emily", lastName: "Davis", email: "emily.davis@school.edu", department: "English" }
      ];

      for (let i = 0; i < mockData.length; i++) {
        const data = mockData[i];
        const result: ImportResult = {
          id: `teacher-${i + 1}`,
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
          await http.post('/admin/teachers', result.data);
          result.status = 'success';
          console.log(`✅ Teacher imported: ${result.data.firstName} ${result.data.lastName}`);
        } catch (error) {
          result.status = 'error';
          result.error = 'Failed to create teacher record';
          console.error(`❌ Failed to import teacher: ${result.data.firstName} ${result.data.lastName}`, error);
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
          toast.success(`Successfully imported ${successful} teachers`);
        } else {
          toast.success(`Imported ${successful} teachers, ${failed} failed`);
        }
      },
      onError: (error) => {
        console.error("❌ useBulkImportTeachers - Error:", error);
        toast.error("Failed to import teachers");
      }
    }
  );
}
