import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { ApiResponse, PageDto } from "@/types/level";
import type { StudentResult } from "./types";

export const STUDENT_RESULTS_PAGE_SIZE = 10;
const SORT = "gradedAt,desc";

/**
 * The student's own results, one page at a time. `page` is zero-based, as in the API.
 * The ID comes from the authenticated session; the backend only answers for the caller's own ID.
 */
export function useStudentResults(studentId: number, page: number) {
  return useQuery({
    queryKey: ["student-results", studentId, page, STUDENT_RESULTS_PAGE_SIZE, SORT],
    gcTime: 0,
    retry: false,
    queryFn: async ({ signal }) => {
      const response = await api.get<ApiResponse<PageDto<StudentResult>>>(
        `/v1/grades/student/${studentId}/paged`,
        { params: { page, size: STUDENT_RESULTS_PAGE_SIZE, sort: SORT }, signal },
      );
      const results = response.data.data;
      if (results.content.some((result) => result.studentId !== studentId)) {
        throw new Error("Unexpected student results response");
      }
      return results;
    },
  });
}
