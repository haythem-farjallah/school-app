// Assignment Management API Hooks
// Comprehensive React Query hooks for assignment operations

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import toast from 'react-hot-toast';

import { http } from '@/lib/http';
import type {
  Assignment,
  CreateAssignmentRequest,
  UpdateAssignmentRequest,
  AssignmentListResponse,
  AssignmentFilters,
  BulkAssignmentOperation,
  AssignmentAnalytics,
  AssignmentTemplate
} from '@/types/assignment';

// API Endpoints
const ENDPOINTS = {
  assignments: '/v1/assignments',
  assignmentById: (id: string) => `/v1/assignments/${id}`,
  assignmentsByTeacher: (teacherId: string) => `/v1/assignments/teacher/${teacherId}`,
  assignmentsByClass: (classId: string) => `/v1/assignments/class/${classId}`,
  assignmentsByCourse: (courseId: string) => `/v1/assignments/course/${courseId}`,
  assignmentAnalytics: (id: string) => `/v1/assignments/${id}/analytics`,
  assignmentTemplates: '/v1/assignments/templates',
  bulkOperations: '/v1/assignments/bulk'
} as const;

// Query Keys Factory
export const assignmentKeys = {
  all: ['assignments'] as const,
  lists: () => [...assignmentKeys.all, 'list'] as const,
  list: (filters: AssignmentFilters) => [...assignmentKeys.lists(), filters] as const,
  details: () => [...assignmentKeys.all, 'detail'] as const,
  detail: (id: string) => [...assignmentKeys.details(), id] as const,
  analytics: (id: string) => [...assignmentKeys.all, 'analytics', id] as const,
  byTeacher: (teacherId: string) => [...assignmentKeys.all, 'teacher', teacherId] as const,
  byClass: (classId: string) => [...assignmentKeys.all, 'class', classId] as const,
  byCourse: (courseId: string) => [...assignmentKeys.all, 'course', courseId] as const,
  templates: () => [...assignmentKeys.all, 'templates'] as const,
} as const;

// API Functions
const assignmentApi = {
  // Get all assignments with filters and pagination
  getAssignments: async (params: {
    page?: number;
    size?: number;
    filters?: AssignmentFilters;
  }): Promise<AssignmentListResponse> => {
    const searchParams = new URLSearchParams();
    
    if (params.page) searchParams.set('page', params.page.toString());
    if (params.size) searchParams.set('size', params.size.toString());
    
    // Add filters to search params
    if (params.filters) {
      Object.entries(params.filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          if (Array.isArray(value)) {
            value.forEach(v => searchParams.append(key, v.toString()));
          } else {
            searchParams.set(key, value.toString());
          }
        }
      });
    }
    
    const response = await http.get(`${ENDPOINTS.assignments}?${searchParams}`);
    return response.data;
  },

  // Get single assignment by ID
  getAssignment: async (id: string): Promise<Assignment> => {
    const response = await http.get(ENDPOINTS.assignmentById(id));
    return response.data;
  },

  // Get assignments by teacher
  getAssignmentsByTeacher: async (teacherId: string, params?: {
    page?: number;
    size?: number;
    filters?: AssignmentFilters;
  }): Promise<AssignmentListResponse> => {
    const searchParams = new URLSearchParams();
    if (params?.page) searchParams.set('page', params.page.toString());
    if (params?.size) searchParams.set('size', params.size.toString());
    
    const response = await http.get(`${ENDPOINTS.assignmentsByTeacher(teacherId)}?${searchParams}`);
    return response.data;
  },

  // Get assignments by class
  getAssignmentsByClass: async (classId: string, params?: {
    page?: number;
    size?: number;
    filters?: AssignmentFilters;
  }): Promise<AssignmentListResponse> => {
    const searchParams = new URLSearchParams();
    if (params?.page) searchParams.set('page', params.page.toString());
    if (params?.size) searchParams.set('size', params.size.toString());
    
    const response = await http.get(`${ENDPOINTS.assignmentsByClass(classId)}?${searchParams}`);
    return response.data;
  },

  // Get assignments by course
  getAssignmentsByCourse: async (courseId: string, params?: {
    page?: number;
    size?: number;
    filters?: AssignmentFilters;
  }): Promise<AssignmentListResponse> => {
    const searchParams = new URLSearchParams();
    if (params?.page) searchParams.set('page', params.page.toString());
    if (params?.size) searchParams.set('size', params.size.toString());
    
    const response = await http.get(`${ENDPOINTS.assignmentsByCourse(courseId)}?${searchParams}`);
    return response.data;
  },

  // Create new assignment
  createAssignment: async (data: CreateAssignmentRequest): Promise<Assignment> => {
    const response = await http.post(ENDPOINTS.assignments, data);
    return response.data;
  },

  // Update assignment
  updateAssignment: async (data: UpdateAssignmentRequest): Promise<Assignment> => {
    const response = await http.put(ENDPOINTS.assignmentById(data.id), data);
    return response.data;
  },

  // Delete assignment
  deleteAssignment: async (id: string): Promise<void> => {
    await http.delete(ENDPOINTS.assignmentById(id));
  },

  // Get assignment analytics
  getAssignmentAnalytics: async (id: string): Promise<AssignmentAnalytics> => {
    const response = await http.get(ENDPOINTS.assignmentAnalytics(id));
    return response.data;
  },

  // Get assignment templates
  getAssignmentTemplates: async (): Promise<AssignmentTemplate[]> => {
    const response = await http.get(ENDPOINTS.assignmentTemplates);
    return response.data;
  },

  // Bulk operations
  bulkOperation: async (operation: BulkAssignmentOperation): Promise<void> => {
    await http.post(ENDPOINTS.bulkOperations, operation);
  },

  // Publish assignment
  publishAssignment: async (id: string): Promise<Assignment> => {
    const response = await http.patch(ENDPOINTS.assignmentById(id), { status: 'published' });
    return response.data;
  },

  // Close assignment
  closeAssignment: async (id: string): Promise<Assignment> => {
    const response = await http.patch(ENDPOINTS.assignmentById(id), { status: 'closed' });
    return response.data;
  },

  // Archive assignment
  archiveAssignment: async (id: string): Promise<Assignment> => {
    const response = await http.patch(ENDPOINTS.assignmentById(id), { status: 'archived' });
    return response.data;
  }
};

// React Query Hooks

/**
 * Hook to get paginated assignments with filters
 */
export function useAssignments(params: {
  page?: number;
  size?: number;
  filters?: AssignmentFilters;
  enabled?: boolean;
} = {}) {
  const { page = 1, size = 10, filters = {}, enabled = true } = params;
  
  return useQuery({
    queryKey: assignmentKeys.list(filters),
    queryFn: () => assignmentApi.getAssignments({ page, size, filters }),
    enabled,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook to get single assignment by ID
 */
export function useAssignment(id: string, enabled = true) {
  return useQuery({
    queryKey: assignmentKeys.detail(id),
    queryFn: () => assignmentApi.getAssignment(id),
    enabled: enabled && !!id,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

/**
 * Hook to get assignments by teacher
 */
export function useAssignmentsByTeacher(teacherId: string, params: {
  page?: number;
  size?: number;
  filters?: AssignmentFilters;
  enabled?: boolean;
} = {}) {
  const { page = 1, size = 10, filters = {}, enabled = true } = params;
  
  return useQuery({
    queryKey: assignmentKeys.byTeacher(teacherId),
    queryFn: () => assignmentApi.getAssignmentsByTeacher(teacherId, { page, size, filters }),
    enabled: enabled && !!teacherId,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

/**
 * Hook to get assignments by class
 */
export function useAssignmentsByClass(classId: string, params: {
  page?: number;
  size?: number;
  filters?: AssignmentFilters;
  enabled?: boolean;
} = {}) {
  const { page = 1, size = 10, filters = {}, enabled = true } = params;
  
  return useQuery({
    queryKey: assignmentKeys.byClass(classId),
    queryFn: () => assignmentApi.getAssignmentsByClass(classId, { page, size, filters }),
    enabled: enabled && !!classId,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

/**
 * Hook to get assignments by course
 */
export function useAssignmentsByCourse(courseId: string, params: {
  page?: number;
  size?: number;
  filters?: AssignmentFilters;
  enabled?: boolean;
} = {}) {
  const { page = 1, size = 10, filters = {}, enabled = true } = params;
  
  return useQuery({
    queryKey: assignmentKeys.byCourse(courseId),
    queryFn: () => assignmentApi.getAssignmentsByCourse(courseId, { page, size, filters }),
    enabled: enabled && !!courseId,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

/**
 * Hook to get assignment analytics
 */
export function useAssignmentAnalytics(id: string, enabled = true) {
  return useQuery({
    queryKey: assignmentKeys.analytics(id),
    queryFn: () => assignmentApi.getAssignmentAnalytics(id),
    enabled: enabled && !!id,
    staleTime: 2 * 60 * 1000, // 2 minutes (analytics change frequently)
    gcTime: 5 * 60 * 1000,
  });
}

/**
 * Hook to get assignment templates
 */
export function useAssignmentTemplates(enabled = true) {
  return useQuery({
    queryKey: assignmentKeys.templates(),
    queryFn: assignmentApi.getAssignmentTemplates,
    enabled,
    staleTime: 30 * 60 * 1000, // 30 minutes (templates don't change often)
    gcTime: 60 * 60 * 1000, // 1 hour
  });
}

// Mutation Hooks

/**
 * Hook to create new assignment
 */
export function useCreateAssignment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: assignmentApi.createAssignment,
    onSuccess: (newAssignment) => {
      // Invalidate and refetch assignment lists
      queryClient.invalidateQueries({ queryKey: assignmentKeys.lists() });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byTeacher(newAssignment.teacherId) });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byClass(newAssignment.classId) });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byCourse(newAssignment.courseId) });
      
      toast.success('Assignment created successfully!');
    },
    onError: (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : 'Failed to create assignment';
      toast.error(errorMessage);
    },
  });
}

/**
 * Hook to update assignment
 */
export function useUpdateAssignment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: assignmentApi.updateAssignment,
    onSuccess: (updatedAssignment) => {
      // Update the assignment in cache
      queryClient.setQueryData(
        assignmentKeys.detail(updatedAssignment.id),
        updatedAssignment
      );
      
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: assignmentKeys.lists() });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byTeacher(updatedAssignment.teacherId) });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byClass(updatedAssignment.classId) });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byCourse(updatedAssignment.courseId) });
      
      toast.success('Assignment updated successfully!');
    },
    onError: (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update assignment';
      toast.error(errorMessage);
    },
  });
}

/**
 * Hook to delete assignment
 */
export function useDeleteAssignment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: assignmentApi.deleteAssignment,
    onSuccess: (_, deletedId) => {
      // Remove from cache
      queryClient.removeQueries({ queryKey: assignmentKeys.detail(deletedId) });
      queryClient.removeQueries({ queryKey: assignmentKeys.analytics(deletedId) });
      
      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: assignmentKeys.lists() });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.all });
      
      toast.success('Assignment deleted successfully!');
    },
    onError: (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : 'Failed to delete assignment';
      toast.error(errorMessage);
    },
  });
}

/**
 * Hook to publish assignment
 */
export function usePublishAssignment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: assignmentApi.publishAssignment,
    onSuccess: (updatedAssignment) => {
      queryClient.setQueryData(
        assignmentKeys.detail(updatedAssignment.id),
        updatedAssignment
      );
      queryClient.invalidateQueries({ queryKey: assignmentKeys.lists() });
      
      toast.success('Assignment published successfully!');
    },
    onError: (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : 'Failed to publish assignment';
      toast.error(errorMessage);
    },
  });
}

/**
 * Hook to close assignment
 */
export function useCloseAssignment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: assignmentApi.closeAssignment,
    onSuccess: (updatedAssignment) => {
      queryClient.setQueryData(
        assignmentKeys.detail(updatedAssignment.id),
        updatedAssignment
      );
      queryClient.invalidateQueries({ queryKey: assignmentKeys.lists() });
      
      toast.success('Assignment closed successfully!');
    },
    onError: (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : 'Failed to close assignment';
      toast.error(errorMessage);
    },
  });
}

/**
 * Hook to archive assignment
 */
export function useArchiveAssignment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: assignmentApi.archiveAssignment,
    onSuccess: (updatedAssignment) => {
      queryClient.setQueryData(
        assignmentKeys.detail(updatedAssignment.id),
        updatedAssignment
      );
      queryClient.invalidateQueries({ queryKey: assignmentKeys.lists() });
      
      toast.success('Assignment archived successfully!');
    },
    onError: (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : 'Failed to archive assignment';
      toast.error(errorMessage);
    },
  });
}

/**
 * Hook for bulk operations
 */
export function useBulkAssignmentOperation() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: assignmentApi.bulkOperation,
    onSuccess: (_, operation) => {
      // Invalidate all assignment queries
      queryClient.invalidateQueries({ queryKey: assignmentKeys.all });
      
      const operationMessages = {
        publish: 'Assignments published successfully!',
        close: 'Assignments closed successfully!',
        extend_deadline: 'Assignment deadlines extended successfully!',
        delete: 'Assignments deleted successfully!',
        archive: 'Assignments archived successfully!',
      };
      
      toast.success(operationMessages[operation.operation] || 'Bulk operation completed successfully!');
    },
    onError: (error: unknown) => {
      const errorMessage = error instanceof Error ? error.message : 'Bulk operation failed';
      toast.error(errorMessage);
    },
  });
}

// Utility Hooks

/**
 * Hook to invalidate assignment queries (useful for real-time updates)
 */
export function useInvalidateAssignments() {
  const queryClient = useQueryClient();
  
  return useCallback(() => {
    queryClient.invalidateQueries({ queryKey: assignmentKeys.all });
  }, [queryClient]);
}

/**
 * Hook to prefetch assignment data
 */
export function usePrefetchAssignment() {
  const queryClient = useQueryClient();
  
  return useCallback((id: string) => {
    queryClient.prefetchQuery({
      queryKey: assignmentKeys.detail(id),
      queryFn: () => assignmentApi.getAssignment(id),
      staleTime: 5 * 60 * 1000,
    });
  }, [queryClient]);
}

// Export API functions for direct use if needed
export { assignmentApi };
