import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import { API_URL } from "@/lib/env";
import { token } from "@/lib/token";
import axios from "axios";
import type { 
  LearningResource, 
  UpdateLearningResourceRequest,
  ResourceUploadRequest,
  LearningResourceFilters,
} from "@/types/learning-resource";

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

      const response = await http.post<LearningResource>("/v1/learning-resources/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      return response.data;
    }
  );
}

/* ── 5. Update resource ──────────────────────────────────────────────────── */
export function useUpdateLearningResource() {
  return useMutationApi<LearningResource, { id: number; data: UpdateLearningResourceRequest }>(
    async ({ id, data }) => {
      const response = await http.put<LearningResource>(`/v1/learning-resources/${id}`, data);
      return response.data;
    }
  );
}

/* ── 6. Delete resource ──────────────────────────────────────────────────── */
export function useDeleteLearningResource() {
  return useMutationApi<void, number>(
    async (resourceId) => {
      await http.delete(`/v1/learning-resources/${resourceId}`);
    }
  );
}

/* ── 7. Download resource file ──────────────────────────────────────────────── */
export function useDownloadResource() {
  return useMutationApi<Blob, string>(
    async (filename) => {
      // Use axios directly to bypass the response interceptor for blob responses
      const response = await axios.get(`${API_URL}/v1/learning-resources/files/${filename}`, {
        responseType: 'blob',
        headers: {
          Authorization: `Bearer ${token.access}`,
        },
      });
      return response.data;
    }
  );
}

/* ── 7b. Preview resource file (increments view count) ──────────────────────── */
export function usePreviewResource() {
  return useMutationApi<Blob, string>(
    async (filename) => {
      // Use axios directly to bypass the response interceptor for blob responses
      const response = await axios.get(`${API_URL}/v1/learning-resources/preview/${filename}`, {
        responseType: 'blob',
        headers: {
          Authorization: `Bearer ${token.access}`,
        },
      });
      return response.data;
    }
  );
}

/* ── Comments ──────────────────────────────────────────────────────── */

 