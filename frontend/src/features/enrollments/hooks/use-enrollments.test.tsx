import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useDropEnrollment, useEnrollment, useEnrollStudent, useUpdateEnrollmentStatus } from "./use-enrollments";
import { apiUrl, server } from "@/test/server";
import { EnrollmentStatus } from "@/types/enrollment";

const enrollment = {
  id: 12,
  studentName: "Sam Student",
  studentEmail: "sam@school.test",
  className: "7A",
  gradeCount: 0,
  enrolledAt: "2026-09-01",
  status: "ACTIVE",
  finalGrad: null,
};

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("enrollment hooks", () => {
  it("reads one enrollment from the envelope", async () => {
    server.use(
      http.get(apiUrl("/v1/enrollments/12"), () => HttpResponse.json({ status: "success", data: enrollment })),
    );
    const { result } = renderHook(() => useEnrollment(12), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(enrollment);
  });

  it("enrolls a student with only the student and class ids and resolves the enrollment", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/enrollments/enroll"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: enrollment });
      }),
    );
    const { result } = renderHook(() => useEnrollStudent(), { wrapper });

    await expect(result.current.mutateAsync({ studentId: 5, classId: 4 })).resolves.toEqual(enrollment);
    expect(body).toEqual({ studentId: 5, classId: 4 });
  });

  it("updates the status and resolves the updated enrollment", async () => {
    let body: unknown;
    server.use(
      http.put(apiUrl("/v1/enrollments/12/status"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: { ...enrollment, status: "SUSPENDED" } });
      }),
    );
    const { result } = renderHook(() => useUpdateEnrollmentStatus(), { wrapper });

    await expect(result.current.mutateAsync({ id: 12, status: EnrollmentStatus.SUSPENDED })).resolves.toEqual({
      ...enrollment,
      status: "SUSPENDED",
    });
    expect(body).toEqual({ status: "SUSPENDED" });
  });

  it("drops an enrollment with the reason in the body and resolves undefined", async () => {
    let body: unknown;
    server.use(
      http.delete(apiUrl("/v1/enrollments/12"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Student dropped from enrollment successfully", data: "" });
      }),
    );
    const { result } = renderHook(() => useDropEnrollment(), { wrapper });

    await expect(result.current.mutateAsync({ id: 12, reason: "Moved to another school" })).resolves.toBeUndefined();
    expect(body).toEqual({ reason: "Moved to another school" });
  });
});
