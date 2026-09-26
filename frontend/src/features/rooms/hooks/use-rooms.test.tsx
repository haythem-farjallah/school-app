import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCreateRoom, useDeleteRoom, useRoom, useUpdateRoom } from "./use-rooms";
import { api } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";
import { RoomType, type Room } from "@/types/room";

const room: Room = { id: 4, name: "Lab 1", capacity: 24, roomType: RoomType.LABORATORY };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("room hooks", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loads a single room through the api client from the backend envelope", async () => {
    const get = vi.spyOn(api, "get");
    server.use(
      http.get(apiUrl("/v1/rooms/4"), () => HttpResponse.json({ status: "Room retrieved successfully", data: room })),
    );

    const { result } = renderHook(() => useRoom(4), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(room);
    expect(get).toHaveBeenCalledOnce();
  });

  it("creates a room through the api client and resolves the room from the backend envelope", async () => {
    const post = vi.spyOn(api, "post");
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/rooms"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Room created successfully", data: room });
      }),
    );
    const { result } = renderHook(() => useCreateRoom(), { wrapper });
    const newRoom = { name: "Lab 1", capacity: 24, roomType: RoomType.LABORATORY };

    await expect(result.current.mutateAsync(newRoom)).resolves.toEqual(room);
    expect(body).toEqual(newRoom);
    expect(post).toHaveBeenCalledOnce();
  });

  it("updates a room through the api client and resolves the room from the backend envelope", async () => {
    const put = vi.spyOn(api, "put");
    const resized = { ...room, capacity: 30 };
    let body: unknown;
    server.use(
      http.put(apiUrl("/v1/rooms/4"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Room updated successfully", data: resized });
      }),
    );
    const { result } = renderHook(() => useUpdateRoom(), { wrapper });
    const changes = { name: "Lab 1", capacity: 30, roomType: RoomType.LABORATORY };

    await expect(result.current.mutateAsync({ id: 4, data: changes })).resolves.toEqual(resized);
    expect(body).toEqual(changes);
    expect(put).toHaveBeenCalledOnce();
  });

  it("deletes a room through the api client and resolves undefined", async () => {
    const remove = vi.spyOn(api, "delete");
    let deleted = false;
    server.use(
      http.delete(apiUrl("/v1/rooms/4"), () => {
        deleted = true;
        return HttpResponse.json({ status: "Room deleted successfully", data: null });
      }),
    );
    const { result } = renderHook(() => useDeleteRoom(), { wrapper });

    await expect(result.current.mutateAsync(4)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
    expect(remove).toHaveBeenCalledOnce();
  });
});
