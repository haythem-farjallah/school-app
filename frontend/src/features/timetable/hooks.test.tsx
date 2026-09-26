import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { usePeriods, useTimetable, useTimetables } from "./hooks";
import { apiUrl, server } from "@/test/server";

const slot = { id: 40, dayOfWeek: "MONDAY", periodId: 1, forClass: { id: 4, name: "7A" } };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("timetable hooks", () => {
  it("reads the periods, which the backend sends without an envelope", async () => {
    const periods = [{ id: 1, index: 1, startTime: "08:00:00", endTime: "09:00:00" }];
    server.use(http.get(apiUrl("/v1/periods"), () => HttpResponse.json(periods)));
    const { result } = renderHook(() => usePeriods(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(periods);
  });

  it("reads a class timetable from the envelope", async () => {
    const timetable = { id: 2, name: "7A timetable", slots: [slot] };
    server.use(
      http.get(apiUrl("/v1/timetables/class/4"), () => HttpResponse.json({ status: "success", data: timetable })),
    );
    const { result } = renderHook(() => useTimetable(4), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(timetable);
  });

  it("treats a class without a timetable (404) as no timetable", async () => {
    server.use(
      http.get(apiUrl("/v1/timetables/class/4"), () =>
        HttpResponse.json({ title: "Not Found", status: 404 }, { status: 404 }),
      ),
    );
    const { result } = renderHook(() => useTimetable(4), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
  });

  it("reports other class timetable failures as errors", async () => {
    server.use(
      http.get(apiUrl("/v1/timetables/class/4"), () =>
        HttpResponse.json({ title: "Forbidden", status: 403 }, { status: 403 }),
      ),
    );
    const { result } = renderHook(() => useTimetable(4), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it("reads the timetable list from the page in the envelope", async () => {
    server.use(
      http.get(apiUrl("/v1/timetables"), () =>
        HttpResponse.json({ status: "success", data: { content: [{ slots: [slot] }], page: 0, size: 10, totalElements: 1 } }),
      ),
    );
    const { result } = renderHook(() => useTimetables(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([{ slots: [slot] }]);
  });
});
