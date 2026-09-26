import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { ExamType, Semester, useCreateBulkEnhancedGrades } from "./use-teacher-grades";
import { apiUrl, server } from "@/test/server";

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("useCreateBulkEnhancedGrades", () => {
  it("posts the grades of a class and resolves the saved grades from the envelope", async () => {
    const saved = [{ id: 70, studentId: 5, classId: 4, courseId: 3, examType: "QUIZ", semester: "FIRST", score: 0, maxScore: 20 }];
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/grades/bulk-entry"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: saved });
      }),
    );
    const { result } = renderHook(() => useCreateBulkEnhancedGrades(), { wrapper });
    const entry = {
      classId: 4,
      courseId: 3,
      examType: ExamType.QUIZ,
      semester: Semester.FIRST,
      grades: [{ studentId: 5, score: 0, maxScore: 20 }],
    };

    await expect(result.current.mutateAsync(entry)).resolves.toEqual(saved);
    expect(body).toEqual(entry);
  });
});
