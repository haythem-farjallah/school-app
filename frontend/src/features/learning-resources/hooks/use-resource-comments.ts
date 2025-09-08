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

// Update a comment
export function useUpdateResourceComment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      commentId, 
      content 
    }: { 
      commentId: number; 
      content: string; 
    }): Promise<ResourceComment> => {
      const response = await http.put(`/resource-comments/${commentId}`, { content });
      return response.data;
    },
    onSuccess: (data) => {
      // Invalidate the comments query for this resource
      queryClient.invalidateQueries({
        queryKey: ["resource-comments", data.resourceId],
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

// Like/Unlike a comment (if this feature exists in backend)
export function useLikeResourceComment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      commentId, 
      isLiked 
    }: { 
      commentId: number; 
      isLiked: boolean; 
    }): Promise<void> => {
      if (isLiked) {
        await http.post(`/resource-comments/${commentId}/like`);
      } else {
        await http.delete(`/resource-comments/${commentId}/like`);
      }
    },
    onSuccess: () => {
      // Invalidate all resource comments queries
      queryClient.invalidateQueries({
        queryKey: ["resource-comments"],
      });
    },
  });
}
