import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useAssignTeacherToCourses,
  useBulkCreateTeachingAssignments,
  useDeleteTeachingAssignment,
} from "./use-teaching-assignments";
import { apiUrl, server } from "@/test/server";

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  queryClient.setQueryData(["teachingAssignments", 0], { content: [] });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const listInvalidated = () => queryClient.getQueryState(["teachingAssignments", 0])?.isInvalidated;
  return { wrapper, listInvalidated };
}

describe("teaching assignment mutations", () => {
  it("assigns a teacher to courses in a class, resolves undefined and refreshes the list", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/teaching-assignments/assign/teacher-to-courses"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: "Teacher assigned to 2 courses" });
      }),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useAssignTeacherToCourses(), { wrapper });

    await expect(result.current.mutateAsync({ teacherId: 9, courseIds: [3, 4], classId: 2 })).resolves.toBeUndefined();
    expect(body).toEqual({ teacherId: 9, courseIds: [3, 4], classId: 2 });
    expect(listInvalidated()).toBe(true);
  });

  it("creates teaching assignments in bulk and resolves undefined", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/teaching-assignments/bulk/create"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: "1 teaching assignments created" });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useBulkCreateTeachingAssignments(), { wrapper });
    const assignments = [{ teacherId: 9, courseId: 3, classId: 2, weeklyHours: 4 }];

    await expect(result.current.mutateAsync(assignments)).resolves.toBeUndefined();
    expect(body).toEqual(assignments);
  });

  it("deletes a teaching assignment and refreshes the list", async () => {
    server.use(
      http.delete(apiUrl("/admin/teaching-assignments/7"), () => HttpResponse.json({ status: "success", data: null })),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useDeleteTeachingAssignment(), { wrapper });

    await expect(result.current.mutateAsync(7)).resolves.toBeUndefined();
    expect(listInvalidated()).toBe(true);
  });
});
