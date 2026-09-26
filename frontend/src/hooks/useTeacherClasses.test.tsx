import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useAllTeacherClasses, useTeacherClassStats } from "./useTeacherClasses";
import { apiUrl, server } from "@/test/server";

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("current teacher class hooks", () => {
  it("reads all of the teacher's classes from the envelope", async () => {
    const teacherClass = { id: 4, name: "7A", courses: [{ id: 3, name: "Mathematics", code: "MATH7", weeklyHours: 4 }] };
    let search: string | null = null;
    server.use(
      http.get(apiUrl("/v1/teacher/classes/all"), ({ request }) => {
        search = new URL(request.url).searchParams.get("search");
        return HttpResponse.json({ status: "success", data: [teacherClass] });
      }),
    );
    const { result } = renderHook(() => useAllTeacherClasses("7"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([teacherClass]);
    expect(search).toBe("7");
  });

  it("reads the teacher's class statistics from the envelope", async () => {
    const stats = { totalClasses: 2, totalStudents: 48, averageGrade: 13.2, totalCapacity: 60, capacityUsed: 80 };
    server.use(http.get(apiUrl("/v1/teacher/classes/stats"), () => HttpResponse.json({ status: "success", data: stats })));
    const { result } = renderHook(() => useTeacherClassStats(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(stats);
  });
});
