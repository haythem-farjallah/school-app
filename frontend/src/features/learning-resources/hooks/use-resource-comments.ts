import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { 
  ResourceComment, 
  CreateResourceCommentRequest
} from "@/types/learning-resource";
import type { ApiResponse, PageDto } from "@/types/level";

// Get comments for a resource
export function useResourceComments(resourceId: number) {
  return useQuery({
    queryKey: ["resource-comments", resourceId],
    queryFn: async (): Promise<PageDto<ResourceComment>> => {
      const response = await api.get<ApiResponse<PageDto<ResourceComment>>>(`/v1/resource-comments/resource/${resourceId}`);
      return response.data.data;
    },
    enabled: !!resourceId,
  });
}

// Create a new comment
export function useCreateResourceComment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (request: CreateResourceCommentRequest): Promise<ResourceComment> => {
      const response = await api.post<ApiResponse<ResourceComment>>("/v1/resource-comments", request);
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      // Invalidate the comments query for this resource
      queryClient.invalidateQueries({
        queryKey: ["resource-comments", variables.resourceId],
      });
    },
  });
}

// Delete a comment
export function useDeleteResourceComment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (commentId: number): Promise<void> => {
      await api.delete(`/v1/resource-comments/${commentId}`);
    },
    onSuccess: () => {
      // Invalidate all resource comments queries
      queryClient.invalidateQueries({
        queryKey: ["resource-comments"],
      });
    },
  });
}
