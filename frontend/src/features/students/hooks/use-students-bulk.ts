import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";
import { useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import type { ImportResult } from "@/components/data-table/bulk-import-dialog";

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
      await http.delete("/v1/students/bulk", { data: ids });
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
      await http.patch('/v1/students/bulk/status', { ids, status, reason });
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
      
      const response = await http.post(endpoint, payload, {
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
      await http.post('/v1/students/bulk/email', {
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

/* ── Bulk Import Students ──────────────────────────────────────────────── */
export function useBulkImportStudents() {
  const queryClient = useQueryClient();
  
  return useMutationApi<ImportResult[], File>(
    async (file) => {
      console.log("📁 useBulkImportStudents - Processing file:", file.name);
      
      // Simulate file processing with progress updates
      const results: ImportResult[] = [];
      
      // Parse file (this would be actual CSV/Excel parsing)
      const mockData = [
        { firstName: "John", lastName: "Doe", email: "john.doe@example.com" },
        { firstName: "Jane", lastName: "Smith", email: "jane.smith@example.com" },
        { firstName: "Bob", lastName: "Johnson", email: "bob.johnson@example.com" },
        { firstName: "Alice", lastName: "Brown", email: "alice.brown@example.com" },
        { firstName: "Charlie", lastName: "Wilson", email: "charlie.wilson@example.com" },
      ];
      
      // Create initial results
      mockData.forEach((data, index) => {
        results.push({
          id: `row-${index + 1}`,
          row: index + 2, // Row 1 is headers
          data,
          status: 'pending'
        });
      });
      
      // Process each row with simulated delay
      for (let i = 0; i < results.length; i++) {
        // Update to processing
        results[i].status = 'processing';
        
        // Simulate processing delay
        await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 2000));
        
        // Simulate success/error (90% success rate)
        if (Math.random() > 0.1) {
          results[i].status = 'success';
          
          // Simulate API call to create student
          try {
            await http.post('/v1/students', {
              profile: {
                firstName: results[i].data.firstName,
                lastName: results[i].data.lastName,
                email: results[i].data.email,
                role: 'STUDENT'
              }
            });
          } catch (error) {
            results[i].status = 'error';
            results[i].error = 'Failed to create student in database';
          }
        } else {
          results[i].status = 'error';
          results[i].error = 'Invalid email format or missing required fields';
        }
      }
      
      return results;
    },
    {
      onSuccess: (results) => {
        const successful = results.filter(r => r.status === 'success').length;
        const failed = results.filter(r => r.status === 'error').length;
        
        queryClient.invalidateQueries({ queryKey: [LIST_KEY] });
        toast.success(`Import completed! ${successful} students added, ${failed} failed`);
      },
      onError: (error) => {
        console.error("❌ useBulkImportStudents - Error:", error);
        toast.error("Failed to process import file");
      }
    }
  );
}
