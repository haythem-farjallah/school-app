// Assignment Submission API Hooks
// React Query hooks for submission operations

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import toast from 'react-hot-toast';

import { http } from '@/lib/http';
import type {
  Submission,
  SubmissionListResponse,
  SubmissionFilters,
  Grade,
  Feedback,
  BulkGradingOperation
} from '@/types/assignment';

// API Endpoints
const ENDPOINTS = {
  submissions: '/v1/submissions',
  submissionById: (id: string) => `/v1/submissions/${id}`,
  submissionsByAssignment: (assignmentId: string) => `/v1/assignments/${assignmentId}/submissions`,
  submissionsByStudent: (studentId: string) => `/v1/students/${studentId}/submissions`,
  submitAssignment: (assignmentId: string) => `/v1/assignments/${assignmentId}/submit`,
  gradeSubmission: (id: string) => `/v1/submissions/${id}/grade`,
  feedbackSubmission: (id: string) => `/v1/submissions/${id}/feedback`,
  bulkGrading: '/v1/submissions/bulk-grade',
  fileUpload: '/v1/submissions/upload',
} as const;

// Query Keys Factory
export const submissionKeys = {
  all: ['submissions'] as const,
  lists: () => [...submissionKeys.all, 'list'] as const,
  list: (filters: SubmissionFilters) => [...submissionKeys.lists(), filters] as const,
  details: () => [...submissionKeys.all, 'detail'] as const,
  detail: (id: string) => [...submissionKeys.details(), id] as const,
  byAssignment: (assignmentId: string) => [...submissionKeys.all, 'assignment', assignmentId] as const,
  byStudent: (studentId: string) => [...submissionKeys.all, 'student', studentId] as const,
} as const;

// API Functions
const submissionApi = {
  // Get submissions with filters and pagination
  getSubmissions: async (params: {
    page?: number;
    size?: number;
    filters?: SubmissionFilters;
  }): Promise<SubmissionListResponse> => {
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
    
    const response = await http.get(`${ENDPOINTS.submissions}?${searchParams}`);
    return response.data;
  },

  // Get single submission by ID
  getSubmission: async (id: string): Promise<Submission> => {
    const response = await http.get(ENDPOINTS.submissionById(id));
    return response.data;
  },

  // Get submissions by assignment
  getSubmissionsByAssignment: async (assignmentId: string, params?: {
    page?: number;
    size?: number;
    filters?: SubmissionFilters;
  }): Promise<SubmissionListResponse> => {
    const searchParams = new URLSearchParams();
    if (params?.page) searchParams.set('page', params.page.toString());
    if (params?.size) searchParams.set('size', params.size.toString());
    
    const response = await http.get(`${ENDPOINTS.submissionsByAssignment(assignmentId)}?${searchParams}`);
    return response.data;
  },

  // Get submissions by student
  getSubmissionsByStudent: async (studentId: string, params?: {
    page?: number;
    size?: number;
    filters?: SubmissionFilters;
  }): Promise<SubmissionListResponse> => {
    const searchParams = new URLSearchParams();
    if (params?.page) searchParams.set('page', params.page.toString());
    if (params?.size) searchParams.set('size', params.size.toString());
    
    const response = await http.get(`${ENDPOINTS.submissionsByStudent(studentId)}?${searchParams}`);
    return response.data;
  },

  // Submit assignment
  submitAssignment: async (assignmentId: string, data: {
    textContent?: string;
    files?: File[];
    urls?: string[];
  }): Promise<Submission> => {
    const formData = new FormData();
    
    if (data.textContent) {
      formData.append('textContent', data.textContent);
    }
    
    if (data.files) {
      data.files.forEach((file, index) => {
        formData.append(`files[${index}]`, file);
      });
    }
    
    if (data.urls) {
      data.urls.forEach((url, index) => {
        formData.append(`urls[${index}]`, url);
      });
    }
    
    const response = await http.post(ENDPOINTS.submitAssignment(assignmentId), formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Update submission
  updateSubmission: async (id: string, data: {
    textContent?: string;
    files?: File[];
    urls?: string[];
  }): Promise<Submission> => {
    const formData = new FormData();
    
    if (data.textContent) {
      formData.append('textContent', data.textContent);
    }
    
    if (data.files) {
      data.files.forEach((file, index) => {
        formData.append(`files[${index}]`, file);
      });
    }
    
    if (data.urls) {
      data.urls.forEach((url, index) => {
        formData.append(`urls[${index}]`, url);
      });
    }
    
    const response = await http.put(ENDPOINTS.submissionById(id), formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Grade submission
  gradeSubmission: async (id: string, gradeData: {
    score: number;
    rubricScores?: Array<{
      criterionId: string;
      levelId: string;
      points: number;
      feedback?: string;
    }>;
    notes?: string;
    isExcused?: boolean;
  }): Promise<Grade> => {
    const response = await http.post(ENDPOINTS.gradeSubmission(id), gradeData);
    return response.data;
  },

  // Add feedback to submission
  addFeedback: async (id: string, feedbackData: {
    generalComments?: string;
    privateComments?: string;
    inlineComments?: Array<{
      startPosition: number;
      endPosition: number;
      comment: string;
    }>;
    fileAnnotations?: Array<{
      fileId: string;
      pageNumber?: number;
      x?: number;
      y?: number;
      width?: number;
      height?: number;
      comment: string;
      type: 'highlight' | 'note' | 'correction';
    }>;
  }): Promise<Feedback> => {
    const response = await http.post(ENDPOINTS.feedbackSubmission(id), feedbackData);
    return response.data;
  },

  // Bulk grading
  bulkGrading: async (operation: BulkGradingOperation): Promise<void> => {
    await http.post(ENDPOINTS.bulkGrading, operation);
  },

  // Upload file
  uploadFile: async (file: File): Promise<{ url: string; fileName: string }> => {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await http.post(ENDPOINTS.fileUpload, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};

// React Query Hooks

/**
 * Hook to get paginated submissions with filters
 */
export function useSubmissions(params: {
  page?: number;
  size?: number;
  filters?: SubmissionFilters;
  enabled?: boolean;
} = {}) {
  const { page = 1, size = 10, filters = {}, enabled = true } = params;
  
  return useQuery({
    queryKey: submissionKeys.list(filters),
    queryFn: () => submissionApi.getSubmissions({ page, size, filters }),
    enabled,
    staleTime: 2 * 60 * 1000, // 2 minutes
    gcTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook to get single submission by ID
 */
export function useSubmission(id: string, enabled = true) {
  return useQuery({
    queryKey: submissionKeys.detail(id),
    queryFn: () => submissionApi.getSubmission(id),
    enabled: enabled && !!id,
    staleTime: 2 * 60 * 1000,
    gcTime: 5 * 60 * 1000,
  });
}

/**
 * Hook to get submissions by assignment
 */
export function useSubmissionsByAssignment(assignmentId: string, params: {
  page?: number;
  size?: number;
  filters?: SubmissionFilters;
  enabled?: boolean;
} = {}) {
  const { page = 1, size = 10, filters = {}, enabled = true } = params;
  
  return useQuery({
    queryKey: submissionKeys.byAssignment(assignmentId),
    queryFn: () => submissionApi.getSubmissionsByAssignment(assignmentId, { page, size, filters }),
    enabled: enabled && !!assignmentId,
    staleTime: 1 * 60 * 1000, // 1 minute (submissions change frequently)
    gcTime: 3 * 60 * 1000,
  });
}

/**
 * Hook to get submissions by student
 */
export function useSubmissionsByStudent(studentId: string, params: {
  page?: number;
  size?: number;
  filters?: SubmissionFilters;
  enabled?: boolean;
} = {}) {
  const { page = 1, size = 10, filters = {}, enabled = true } = params;
  
  return useQuery({
    queryKey: submissionKeys.byStudent(studentId),
    queryFn: () => submissionApi.getSubmissionsByStudent(studentId, { page, size, filters }),
    enabled: enabled && !!studentId,
    staleTime: 2 * 60 * 1000,
    gcTime: 5 * 60 * 1000,
  });
}

// Mutation Hooks

/**
 * Hook to submit assignment
 */
export function useSubmitAssignment() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ assignmentId, data }: {
      assignmentId: string;
      data: {
        textContent?: string;
        files?: File[];
        urls?: string[];
      };
    }) => submissionApi.submitAssignment(assignmentId, data),
    onSuccess: (newSubmission) => {
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: submissionKeys.byAssignment(newSubmission.assignmentId) });
      queryClient.invalidateQueries({ queryKey: submissionKeys.byStudent(newSubmission.studentId) });
      queryClient.invalidateQueries({ queryKey: submissionKeys.lists() });
      
      toast.success('Assignment submitted successfully!');
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || 'Failed to submit assignment');
    },
  });
}

/**
 * Hook to update submission
 */
export function useUpdateSubmission() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, data }: {
      id: string;
      data: {
        textContent?: string;
        files?: File[];
        urls?: string[];
      };
    }) => submissionApi.updateSubmission(id, data),
    onSuccess: (updatedSubmission) => {
      // Update the submission in cache
      queryClient.setQueryData(
        submissionKeys.detail(updatedSubmission.id),
        updatedSubmission
      );
      
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: submissionKeys.byAssignment(updatedSubmission.assignmentId) });
      queryClient.invalidateQueries({ queryKey: submissionKeys.byStudent(updatedSubmission.studentId) });
      
      toast.success('Submission updated successfully!');
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || 'Failed to update submission');
    },
  });
}

/**
 * Hook to grade submission
 */
export function useGradeSubmission() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, gradeData }: {
      id: string;
      gradeData: {
        score: number;
        rubricScores?: Array<{
          criterionId: string;
          levelId: string;
          points: number;
          feedback?: string;
        }>;
        notes?: string;
        isExcused?: boolean;
      };
    }) => submissionApi.gradeSubmission(id, gradeData),
    onSuccess: (_, { id }) => {
      // Invalidate submission and related queries
      queryClient.invalidateQueries({ queryKey: submissionKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: submissionKeys.lists() });
      
      toast.success('Submission graded successfully!');
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || 'Failed to grade submission');
    },
  });
}

/**
 * Hook to add feedback to submission
 */
export function useAddFeedback() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, feedbackData }: {
      id: string;
      feedbackData: {
        generalComments?: string;
        privateComments?: string;
        inlineComments?: Array<{
          startPosition: number;
          endPosition: number;
          comment: string;
        }>;
        fileAnnotations?: Array<{
          fileId: string;
          pageNumber?: number;
          x?: number;
          y?: number;
          width?: number;
          height?: number;
          comment: string;
          type: 'highlight' | 'note' | 'correction';
        }>;
      };
    }) => submissionApi.addFeedback(id, feedbackData),
    onSuccess: (_, { id }) => {
      // Invalidate submission queries
      queryClient.invalidateQueries({ queryKey: submissionKeys.detail(id) });
      
      toast.success('Feedback added successfully!');
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || 'Failed to add feedback');
    },
  });
}

/**
 * Hook for bulk grading operations
 */
export function useBulkGrading() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: submissionApi.bulkGrading,
    onSuccess: (_, operation) => {
      // Invalidate all submission queries
      queryClient.invalidateQueries({ queryKey: submissionKeys.all });
      
      const operationMessages = {
        grade: 'Submissions graded successfully!',
        return: 'Submissions returned successfully!',
        excuse: 'Submissions excused successfully!',
        reset: 'Submissions reset successfully!',
      };
      
      toast.success(operationMessages[operation.operation] || 'Bulk operation completed successfully!');
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || 'Bulk operation failed');
    },
  });
}

/**
 * Hook to upload file
 */
export function useUploadFile() {
  return useMutation({
    mutationFn: submissionApi.uploadFile,
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || 'Failed to upload file');
    },
  });
}

// Utility Hooks

/**
 * Hook to invalidate submission queries
 */
export function useInvalidateSubmissions() {
  const queryClient = useQueryClient();
  
  return useCallback(() => {
    queryClient.invalidateQueries({ queryKey: submissionKeys.all });
  }, [queryClient]);
}

/**
 * Hook to prefetch submission data
 */
export function usePrefetchSubmission() {
  const queryClient = useQueryClient();
  
  return useCallback((id: string) => {
    queryClient.prefetchQuery({
      queryKey: submissionKeys.detail(id),
      queryFn: () => submissionApi.getSubmission(id),
      staleTime: 2 * 60 * 1000,
    });
  }, [queryClient]);
}

// Export API functions for direct use if needed
export { submissionApi };
