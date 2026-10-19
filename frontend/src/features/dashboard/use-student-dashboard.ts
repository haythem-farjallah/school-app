import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { ApiResponse } from "@/types/level";

import type { StudentDashboardData } from "./types";

export function useStudentDashboard(accountId: number) {
  return useQuery({
    // The ID isolates the cache; the server selects the student from authentication.
    queryKey: ["student-dashboard", accountId],
    gcTime: 0,
    retry: false,
    queryFn: async ({ signal }) => {
      const response = await api.get<ApiResponse<StudentDashboardData>>(
        "/v1/dashboard/current-user",
        { signal },
      );
      const dashboard = response.data.data;
      if (
        dashboard.type !== "STUDENT" ||
        dashboard.baseInfo.userInfo.id !== accountId
      ) {
        throw new Error("Unexpected student dashboard response");
      }
      return dashboard;
    },
  });
}
