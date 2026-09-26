import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { api } from "@/lib/api-client";
import type { 
  LearningResource, 
  UpdateLearningResourceRequest,
  ResourceUploadRequest,
  LearningResourceFilters,
} from "@/types/learning-resource";
import type { ApiResponse } from "@/types/level";

const LIST_KEY = "learning-resources";

/* ── 1. Paginated list ──────────────────────────────────────────────────── */
export function useLearningResources(
  options: { size?: number } & LearningResourceFilters = {},
) {
  const { size = 10, ...filters } = options;

  return usePaginated<LearningResource>(
    "/v1/learning-resources",
    LIST_KEY,
    size,
    filters,
  );
}

/* ── 4. Upload resource (File) ──────────────────────────────────────────────── */
export function useUploadLearningResource() {
  return useMutationApi<LearningResource, ResourceUploadRequest>(
    async (uploadData) => {
      const formData = new FormData();
      formData.append("file", uploadData.file);
      formData.append("title", uploadData.title);
      formData.append("description", uploadData.description);
      formData.append("type", uploadData.type);
      formData.append("isPublic", uploadData.isPublic.toString());
      
      if (uploadData.thumbnailUrl) {
        formData.append("thumbnailUrl", uploadData.thumbnailUrl);
      }
      if (uploadData.duration) {
        formData.append("duration", uploadData.duration.toString());
      }
      if (uploadData.classIds) {
        formData.append("classIds", JSON.stringify(uploadData.classIds));
      }
      if (uploadData.courseIds) {
        formData.append("courseIds", JSON.stringify(uploadData.courseIds));
      }

      const response = await api.post<ApiResponse<LearningResource>>("/v1/learning-resources/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      return response.data.data;
    }
  );
}

/* ── 5. Update resource ──────────────────────────────────────────────────── */
export function useUpdateLearningResource() {
  return useMutationApi<LearningResource, { id: number; data: UpdateLearningResourceRequest }>(
    async ({ id, data }) => {
      const response = await api.put<ApiResponse<LearningResource>>(`/v1/learning-resources/${id}`, data);
      return response.data.data;
    }
  );
}

/* ── 6. Delete resource ──────────────────────────────────────────────────── */
export function useDeleteLearningResource() {
  return useMutationApi<void, number>(
    async (resourceId) => {
      await api.delete(`/v1/learning-resources/${resourceId}`);
    }
  );
}

/* ── 7. Download resource file ──────────────────────────────────────────────── */
export function useDownloadResource() {
  return useMutationApi<Blob, string>(
    async (filename) => {
      const response = await api.get<Blob>(`/v1/learning-resources/files/${filename}`, {
        responseType: 'blob',
      });
      return response.data;
    }
  );
}

/* ── 7b. Preview resource file (increments view count) ──────────────────────── */
export function usePreviewResource() {
  return useMutationApi<Blob, string>(
    async (filename) => {
      const response = await api.get<Blob>(`/v1/learning-resources/preview/${filename}`, {
        responseType: 'blob',
      });
      return response.data;
    }
  );
}

/* ── Comments ──────────────────────────────────────────────────────── */

 