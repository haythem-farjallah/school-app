import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useTeacherSchedule } from "./use-schedule";
import { apiUrl, server } from "@/test/server";

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("useTeacherSchedule", () => {
  it("reads the teacher's timetable slots from the envelope", async () => {
    const slot = { id: 40, dayOfWeek: "MONDAY", periodId: 1, teacher: { id: 9 } };
    server.use(
      http.get(apiUrl("/v1/timetables/teacher/9"), () => HttpResponse.json({ status: "success", data: [slot] })),
    );
    const { result } = renderHook(() => useTeacherSchedule(9), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([slot]);
  });

  it("reports a failed request as an error instead of an empty schedule", async () => {
    server.use(
      http.get(apiUrl("/v1/timetables/teacher/9"), () =>
        HttpResponse.json({ title: "Internal Server Error", status: 500 }, { status: 500 }),
      ),
    );
    const { result } = renderHook(() => useTeacherSchedule(9), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
