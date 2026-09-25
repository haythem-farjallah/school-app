import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { usePaginated } from "./usePaginated";
import { apiUrl, server } from "@/test/server";
import { store as appStore } from "@/stores/store";

interface Room {
  id: number;
  name: string;
}

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

/** Answers like the backend: ApiResponse<PageDto<Room>>, recording the query string. */
function roomsPage(requests: URLSearchParams[], content: Room[], page: number, size: number, totalElements: number) {
  return http.get(apiUrl("/v1/rooms"), ({ request }) => {
    requests.push(new URL(request.url).searchParams);
    return HttpResponse.json({ status: "success", data: { content, page, size, totalElements } });
  });
}

describe("usePaginated", () => {
  it("maps the backend PageDto to a Page and sends page, size and filters", async () => {
    const requests: URLSearchParams[] = [];
    const rooms = [{ id: 1, name: "Lab" }, { id: 2, name: "Hall" }];
    server.use(roomsPage(requests, rooms, 0, 2, 5));

    const { result } = renderHook(() => usePaginated<Room>("/v1/rooms", "rooms", 2, { nameLike: "a" }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ data: rooms, page: 0, totalPages: 3, totalItems: 5 });
    expect(Object.fromEntries(requests[0])).toEqual({ page: "0", size: "2", nameLike: "a" });
  });

  it("requests the external page when one is given", async () => {
    const requests: URLSearchParams[] = [];
    server.use(roomsPage(requests, [], 3, 10, 30));

    const { result } = renderHook(() => usePaginated<Room>("/v1/rooms", "rooms", 10, {}, 3), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.page).toBe(3);
    expect(requests[0].get("page")).toBe("3");
    expect(result.current.data).toEqual({ data: [], page: 3, totalPages: 3, totalItems: 30 });
  });

  it("exposes a failed request as the query error without showing a notification", async () => {
    server.use(
      http.get(apiUrl("/v1/rooms"), () =>
        HttpResponse.json(
          { type: "about:blank", title: "Internal Server Error", status: 500, detail: "UNEXPECTED_ERROR", instance: "/api/v1/rooms" },
          { status: 500 },
        ),
      ),
    );
    const notificationsBefore = appStore.getState().notification.list.length;

    const { result } = renderHook(() => usePaginated<Room>("/v1/rooms", "rooms"), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error?.response?.status).toBe(500);
    expect(appStore.getState().notification.list).toHaveLength(notificationsBefore);
  });
});
