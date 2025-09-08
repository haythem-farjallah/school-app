import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { useQueryApi } from "@/hooks/useQueryApi";
import { useQueryClient } from "@tanstack/react-query";
import { http } from "@/lib/http";
import toast from "react-hot-toast";
import { useCallback, useMemo } from "react";

/**
 * Configuration for generic user operations
 */
export interface UserOperationsConfig {
  // Entity configuration
  entityType: 'teachers' | 'students' | 'staff';
  entityDisplayName: string;
  entityDisplayNamePlural: string;
  
  // API endpoints
  baseEndpoint: string; // e.g., "/admin/teachers"
  
  // Query keys
  listQueryKey: string; // e.g., "teachers"
  
  // API parameter mapping for filtering
  filterParamMap: Record<string, string>;
}

/**
 * Generic hook for user CRUD operations
 */
export function useGenericUserOperations<T>(config: UserOperationsConfig) {
  const queryClient = useQueryClient();

  // Data fetching hook
  const useData = useCallback((
    options: { page?: number; size?: number; search?: string } & Record<string, unknown> = {}
  ) => {
    const { page, size = 10, search, ...filters } = options;

    console.log(`🔍 use${config.entityDisplayNamePlural} - Called with options:`, { size, search, filters });

    // Map frontend column keys to backend filter parameter names
    const apiParams = useMemo(() => {
      const params: Record<string, unknown> = {};

      Object.entries(filters).forEach(([key, val]) => {
        if (typeof val === "string" && val.trim()) {
          const backendKey = config.filterParamMap[key] ?? key;
          params[backendKey] = val.trim();
        }
      });

      return params;
    }, [filters]);

    // Add search to filters if provided
    const searchFilters = search ? { search, ...apiParams } : apiParams;

    const result = usePaginated<T>(
      config.baseEndpoint,
      config.listQueryKey,
      size,
      searchFilters,
      false,
      page, // external page number
    );

    console.log(`🔍 use${config.entityDisplayNamePlural} - Result:`, {
      data: result.data,
      isLoading: result.isLoading,
      error: result.error?.message,
      totalElements: result.data?.totalItems,
      totalPages: result.data?.totalPages
    });

    return result;
  }, [config]);

  // Single entity fetch
  const useEntity = useCallback((id?: number) => {
    return useQueryApi<T>(
      [config.listQueryKey, id],
      () => http.get(`${config.baseEndpoint}/${id}`).then(res => res.data),
      {
        enabled: !!id,
        placeholderData: (prev) => prev,
      }
    );
  }, [config]);

  // Delete hook
  const useDelete = useCallback(() => {
    return useMutationApi<void, number>(
      async (id: number) => {
        console.log(`🗑️ useDelete${config.entityDisplayName} - Deleting:`, id);
        await http.delete(`${config.baseEndpoint}/${id}`);
      },
      {
        onSuccess: (_, id) => {
          console.log(`✅ useDelete${config.entityDisplayName} - Deleted successfully:`, id);
          queryClient.invalidateQueries({ queryKey: [config.listQueryKey] });
          toast.success(`${config.entityDisplayName} deleted successfully`);
        },
        onError: (error, id) => {
          console.error(`❌ useDelete${config.entityDisplayName} - Error:`, error);
          toast.error(`Failed to delete ${config.entityDisplayName.toLowerCase()}`);
        }
      }
    );
  }, [config, queryClient]);

  // Bulk operations hook
  const useBulkOperations = useCallback(() => {
    const bulkDelete = useMutationApi<void, number[]>(
      async (ids: number[]) => {
        console.log(`🗑️ useBulkDelete${config.entityDisplayNamePlural} - Deleting:`, ids);
        await http.delete(`${config.baseEndpoint}/bulk`, { data: ids });
      },
      {
        onSuccess: (_, ids) => {
          console.log(`✅ useBulkDelete${config.entityDisplayNamePlural} - Deleted successfully:`, ids);
          queryClient.invalidateQueries({ queryKey: [config.listQueryKey] });
          toast.success(`${ids.length} ${config.entityDisplayNamePlural.toLowerCase()} deleted successfully`);
        },
        onError: (error) => {
          console.error(`❌ useBulkDelete${config.entityDisplayNamePlural} - Error:`, error);
          toast.error(`Failed to delete ${config.entityDisplayNamePlural.toLowerCase()}`);
        }
      }
    );

    const bulkStatusUpdate = useMutationApi<void, { ids: number[]; status: string; reason?: string }>(
      async ({ ids, status, reason }) => {
        console.log(`✏️ useBulkUpdate${config.entityDisplayName}Status - Updating:`, { ids, status, reason });
        await http.patch(`${config.baseEndpoint}/bulk/status`, { ids, status, reason });
      },
      {
        onSuccess: (_, { ids, status }) => {
          console.log(`✅ useBulkUpdate${config.entityDisplayName}Status - Updated successfully:`, { ids, status });
          queryClient.invalidateQueries({ queryKey: [config.listQueryKey] });
          toast.success(`${ids.length} ${config.entityDisplayNamePlural.toLowerCase()} status updated to ${status}`);
        },
        onError: (error) => {
          console.error(`❌ useBulkUpdate${config.entityDisplayName}Status - Error:`, error);
          toast.error(`Failed to update ${config.entityDisplayNamePlural.toLowerCase()} status`);
        }
      }
    );

    const bulkExport = useMutationApi<{ data: string | ArrayBuffer; filename: string }, { ids?: number[]; format: 'csv' | 'xlsx' }>(
      async ({ ids, format }) => {
        console.log(`📊 useBulkExport${config.entityDisplayNamePlural} - Exporting:`, { ids, format });

        const endpoint = format === 'csv' ? 
          `${config.baseEndpoint}/export/csv` : 
          `${config.baseEndpoint}/export/excel`;
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
        const filename = filenameMatch ? filenameMatch[1] : 
          `${config.entityType}_export_${new Date().toISOString().split('T')[0]}.${format}`;

        return {
          data: response.data,
          filename
        };
      },
      {
        onSuccess: ({ data, filename }, { format }) => {
          console.log(`✅ useBulkExport${config.entityDisplayNamePlural} - Export successful, downloading file:`, filename);

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

          toast.success(`${config.entityDisplayNamePlural} exported successfully as ${format.toUpperCase()}`);
        },
        onError: (error) => {
          console.error(`❌ useBulkExport${config.entityDisplayNamePlural} - Error:`, error);
          toast.error(`Failed to export ${config.entityDisplayNamePlural.toLowerCase()}`);
        }
      }
    );

    const bulkEmail = useMutationApi<void, { ids: number[]; subject: string; message: string; actionUrl?: string; actionText?: string }>(
      async ({ ids, subject, message, actionUrl, actionText }) => {
        console.log(`📧 useBulkEmail${config.entityDisplayNamePlural} - Sending email:`, { ids, subject });
        await http.post(`${config.baseEndpoint}/bulk/email`, { ids, subject, message, actionUrl, actionText });
      },
      {
        onSuccess: (_, { ids }) => {
          console.log(`✅ useBulkEmail${config.entityDisplayNamePlural} - Email sent successfully to:`, ids);
          toast.success(`Bulk email sent to ${ids.length} ${config.entityDisplayNamePlural.toLowerCase()}`);
        },
        onError: (error) => {
          console.error(`❌ useBulkEmail${config.entityDisplayNamePlural} - Error:`, error);
          toast.error(`Failed to send bulk email to ${config.entityDisplayNamePlural.toLowerCase()}`);
        }
      }
    );

    const bulkImport = useMutationApi<void, File>(
      async (file: File) => {
        console.log(`📁 useBulkImport${config.entityDisplayNamePlural} - Importing from file:`, file.name);
        const formData = new FormData();
        formData.append('file', file);
        await http.post(`${config.baseEndpoint}/bulk/import`, formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        });
      },
      {
        onSuccess: (_, file) => {
          console.log(`✅ useBulkImport${config.entityDisplayNamePlural} - Import successful:`, file.name);
          queryClient.invalidateQueries({ queryKey: [config.listQueryKey] });
          toast.success(`${config.entityDisplayNamePlural} imported successfully`);
        },
        onError: (error) => {
          console.error(`❌ useBulkImport${config.entityDisplayNamePlural} - Error:`, error);
          toast.error(`Failed to import ${config.entityDisplayNamePlural.toLowerCase()}`);
        }
      }
    );

    return {
      bulkDelete,
      bulkStatusUpdate,
      bulkExport,
      bulkEmail,
      bulkImport,
    };
  }, [config, queryClient]);

  return {
    useData,
    useEntity,
    useDelete,
    useBulkOperations,
    config,
  };
}
