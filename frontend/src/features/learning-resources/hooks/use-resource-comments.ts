import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { http } from "@/lib/http";
import type { 
  ResourceComment, 
  CreateResourceCommentRequest, 
  ResourceCommentResponse 
} from "@/types/learning-resource";

// Get comments for a resource
export function useResourceComments(resourceId: number) {
  return useQuery({
    queryKey: ["resource-comments", resourceId],
    queryFn: async (): Promise<ResourceCommentResponse> => {
      const response = await http.get(`/resource-comments/resource/${resourceId}`);
      return response.data;
    },
    enabled: !!resourceId,
  });
}

// Create a new comment
export function useCreateResourceComment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (request: CreateResourceCommentRequest): Promise<ResourceComment> => {
      const response = await http.post("/resource-comments", request);
      return response.data;
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
      await http.delete(`/resource-comments/${commentId}`);
    },
    onSuccess: () => {
      // Invalidate all resource comments queries
      queryClient.invalidateQueries({
        queryKey: ["resource-comments"],
      });
    },
  });
}
