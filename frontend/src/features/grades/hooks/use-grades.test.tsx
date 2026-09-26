import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useApproveGrades,
  useExportGradeSheet,
  useStaffGradeReviews,
  useStudentGradeSheet,
  useTeacherAttendance,
} from "./use-grades";
import { apiUrl, server } from "@/test/server";
import { Semester } from "@/types/grade";

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("grade review and grade sheet hooks", () => {
  it("reads the staff grade reviews for a class and semester from the envelope", async () => {
    const review = { studentId: 5, studentFirstName: "Sam", classId: 4, semester: "FIRST", overallAverage: 14.5, isApproved: false };
    let params: URLSearchParams | undefined;
    server.use(
      http.get(apiUrl("/v1/grades/staff/reviews"), ({ request }) => {
        params = new URL(request.url).searchParams;
        return HttpResponse.json({ status: "success", data: [review] });
      }),
    );
    const { result } = renderHook(() => useStaffGradeReviews(4, Semester.FIRST), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([review]);
    expect(params?.get("classId")).toBe("4");
    expect(params?.get("semester")).toBe("FIRST");
  });

  it("approves grades and resolves undefined", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/grades/approve"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Grades approved successfully", data: null });
      }),
    );
    const { result } = renderHook(() => useApproveGrades(), { wrapper });
    const approval = { studentIds: [5], semester: Semester.FIRST, approvedBy: "Ada Admin" };

    await expect(result.current.mutateAsync(approval)).resolves.toBeUndefined();
    expect(body).toEqual(approval);
  });

  it("reads a student's grade sheet from the envelope", async () => {
    const sheet = { studentId: 5, studentFirstName: "Sam", className: "7A", subjects: [] };
    server.use(
      http.get(apiUrl("/v1/grades/student/5/sheet"), () => HttpResponse.json({ status: "success", data: sheet })),
    );
    const { result } = renderHook(() => useStudentGradeSheet(5, Semester.FIRST), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(sheet);
  });

  it("exports the grade sheet as the file blob", async () => {
    server.use(
      http.get(apiUrl("/v1/grades/student/5/export"), () =>
        new HttpResponse("%PDF-sheet", { headers: { "Content-Type": "application/pdf" } }),
      ),
    );
    const { result } = renderHook(() => useExportGradeSheet(), { wrapper });

    const blob = await result.current.mutateAsync({ studentId: 5, semester: Semester.FIRST });

    expect(blob).toBeInstanceOf(Blob);
    expect(blob.size).toBe("%PDF-sheet".length);
  });

  it("reads teacher attendance records from the envelope", async () => {
    const record = { id: 1, teacherId: 9, teacherFirstName: "Tia", date: "2026-09-21", status: "PRESENT" };
    server.use(
      http.get(apiUrl("/v1/teacher-attendance"), () => HttpResponse.json({ status: "success", data: [record] })),
    );
    const { result } = renderHook(() => useTeacherAttendance({ teacherId: 9 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([record]);
  });
});
