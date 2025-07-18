import { useMutationApi } from "@/hooks/useMutationApi";
import { usePaginated } from "@/hooks/usePaginated";
import { http } from "@/lib/http";
import type { 
  LearningResource, 
  CreateLearningResourceRequest, 
  UpdateLearningResourceRequest,
  ResourceUploadRequest,
  ResourceComment,
  CreateResourceCommentRequest,
  LearningResourceFilters,
} from "@/types/learning-resource";
import { useQueryApi } from "@/hooks/useQueryApi";

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

/* ── 2. Single resource ──────────────────────────────────────────────────── */
export function useLearningResource(id?: number) {
  return useQueryApi<LearningResource>(
    ["learning-resource", id],
    () => http.get<LearningResource, LearningResource>(`/v1/learning-resources/${id}`),
    { enabled: !!id },
  );
}

/* ── 3. Create resource (URL) ──────────────────────────────────────────────── */
export function useCreateLearningResource() {
  return useMutationApi<LearningResource, CreateLearningResourceRequest>(
    async (resourceData) => {
      const response = await http.post<LearningResource>("/v1/learning-resources", resourceData);
      return response.data;
    }
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
      const response = await http.get(`/v1/learning-resources/files/${filename}`, {
        responseType: 'blob',
      });
      return response.data;
    }
  );
}

/* ── 8. Add target classes ──────────────────────────────────────────────────── */
export function useAddTargetClasses() {
  return useMutationApi<void, { resourceId: number; classIds: number[] }>(
    async ({ resourceId, classIds }) => {
      await http.post(`/v1/learning-resources/${resourceId}/classes`, classIds);
    }
  );
}

/* ── 9. Add target courses ──────────────────────────────────────────────────── */
export function useAddTargetCourses() {
  return useMutationApi<void, { resourceId: number; courseIds: number[] }>(
    async ({ resourceId, courseIds }) => {
      await http.post(`/v1/learning-resources/${resourceId}/courses`, courseIds);
    }
  );
}

/* ── Comments ──────────────────────────────────────────────────────── */

/* ── 10. Resource comments ──────────────────────────────────────────────── */
export function useResourceComments(
  options: { resourceId?: number; size?: number } = {},
) {
  const { resourceId, size = 10 } = options;

  return usePaginated<ResourceComment>(
    resourceId ? `/v1/resource-comments/resource/${resourceId}` : "/v1/resource-comments",
    `resource-comments-${resourceId || 'all'}`,
    size,
    {},
  );
}

/* ── 11. Create comment ──────────────────────────────────────────────────── */
export function useCreateResourceComment() {
  return useMutationApi<ResourceComment, CreateResourceCommentRequest>(
    async (commentData) => {
      const response = await http.post<ResourceComment>("/v1/resource-comments", commentData);
      return response.data;
    }
  );
}

/* ── 12. Delete comment ──────────────────────────────────────────────────── */
export function useDeleteResourceComment() {
  return useMutationApi<void, number>(
    async (commentId) => {
      await http.delete(`/v1/resource-comments/${commentId}`);
    }
  );
} 