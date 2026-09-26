import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useAttendanceRecords,
  useMarkAttendanceForSlot,
  useStudentsForClass,
  useTeacherAbsentStudents,
  useUpdateAttendance,
} from "./use-attendance";
import { apiUrl, server } from "@/test/server";
import { AttendanceStatus, UserType } from "@/types/attendance";

const record = { id: 3, userId: 11, userName: "Sam Student", date: "2026-09-21", status: "ABSENT", userType: "STUDENT" };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("attendance hooks", () => {
  it("reads the filtered page from the envelope into the table's list shape", async () => {
    let query = "";
    server.use(
      http.get(apiUrl("/v1/attendance/filter"), ({ request }) => {
        query = new URL(request.url).search;
        return HttpResponse.json({ status: "success", data: { content: [record], page: 1, size: 10, totalElements: 21 } });
      }),
    );
    const { result } = renderHook(() => useAttendanceRecords({ classId: 4 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ data: [record], totalItems: 21, totalPages: 3, currentPage: 1 });
    expect(new URLSearchParams(query).get("classId")).toBe("4");
  });

  it("returns the class students from the envelope", async () => {
    server.use(
      http.get(apiUrl("/v1/attendance/class/4/students-simple"), () =>
        HttpResponse.json({ status: "success", data: [record] }),
      ),
    );
    const { result } = renderHook(() => useStudentsForClass(4), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([record]);
  });

  it("reports a failed absent-students request as an error instead of an empty list", async () => {
    server.use(
      http.get(apiUrl("/v1/attendance/teacher/9/absent-students"), () =>
        HttpResponse.json({ title: "Forbidden", status: 403 }, { status: 403 }),
      ),
    );
    const { result } = renderHook(() => useTeacherAbsentStudents(9, "2026-09-21"), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });

  it("updates a record with PUT, the only update mapping, and resolves the updated record", async () => {
    let body: unknown;
    server.use(
      http.put(apiUrl("/v1/attendance/3"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: { ...record, status: "EXCUSED" } });
      }),
    );
    const { result } = renderHook(() => useUpdateAttendance(), { wrapper });
    const data = { userId: 11, date: "2026-09-21", status: AttendanceStatus.EXCUSED, userType: UserType.STUDENT };

    await expect(result.current.mutateAsync({ id: 3, data })).resolves.toEqual({ ...record, status: "EXCUSED" });
    expect(body).toEqual(data);
  });

  it("marks a slot for the given date and resolves the saved records", async () => {
    let date: string | null = null;
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/attendance/slot/5/mark"), async ({ request }) => {
        date = new URL(request.url).searchParams.get("date");
        body = await request.json();
        return HttpResponse.json({ status: "success", data: [record] });
      }),
    );
    const { result } = renderHook(() => useMarkAttendanceForSlot(), { wrapper });
    const attendanceList = [
      { userId: 11, timetableSlotId: 5, date: "2026-09-21", status: AttendanceStatus.ABSENT, userType: UserType.STUDENT },
    ];

    await expect(result.current.mutateAsync({ slotId: 5, date: "2026-09-21", attendanceList })).resolves.toEqual([record]);
    expect(date).toBe("2026-09-21");
    expect(body).toEqual(attendanceList);
  });
});
