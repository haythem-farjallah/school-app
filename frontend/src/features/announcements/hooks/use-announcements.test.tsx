import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useAnnouncements,
  useCreateAnnouncement,
  useDeleteAnnouncement,
  usePublicAnnouncements,
  useUpdateAnnouncement,
} from "./use-announcements";
import { apiUrl, server } from "@/test/server";
import type { Announcement, CreateAnnouncementRequest } from "@/types/announcement";
import type { PageDto } from "@/types/level";

const announcement: Announcement = {
  id: 9,
  title: "Exam week",
  body: "Exams start on Monday.",
  isPublic: true,
  importance: "HIGH",
  createdAt: "2026-09-01T08:00:00",
  createdById: 1,
  createdByName: "Ada Admin",
  targetType: "WHOLE_SCHOOL",
};

const page: PageDto<Announcement> = { content: [announcement], page: 0, size: 50, totalElements: 1 };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("announcement hooks", () => {
  it("lists announcements with the filters as query parameters and resolves the page from the backend envelope", async () => {
    let query: URLSearchParams | undefined;
    server.use(
      http.get(apiUrl("/v1/announcements"), ({ request }) => {
        query = new URL(request.url).searchParams;
        return HttpResponse.json({ status: "success", data: page });
      }),
    );

    const { result } = renderHook(
      () => useAnnouncements({ page: 0, size: 50, importance: "HIGH", isPublic: true }),
      { wrapper },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(page);
    expect(Object.fromEntries(query!)).toEqual({ page: "0", size: "50", importance: "HIGH", isPublic: "true" });
  });

  it("lists public announcements for the requested page and resolves the page from the backend envelope", async () => {
    let query: URLSearchParams | undefined;
    server.use(
      http.get(apiUrl("/v1/announcements/public"), ({ request }) => {
        query = new URL(request.url).searchParams;
        return HttpResponse.json({ status: "success", data: page });
      }),
    );

    const { result } = renderHook(() => usePublicAnnouncements(0, 50), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(page);
    expect(Object.fromEntries(query!)).toEqual({ page: "0", size: "50" });
  });

  it("posts the create request and resolves the announcement from the backend envelope", async () => {
    const request: CreateAnnouncementRequest = {
      title: "Exam week",
      body: "Exams start on Monday.",
      isPublic: true,
      importance: "HIGH",
      targetType: "CLASSES",
      targetClassIds: [4],
      sendNotifications: true,
    };
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/announcements"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: announcement });
      }),
    );
    const { result } = renderHook(() => useCreateAnnouncement(), { wrapper });

    await expect(result.current.mutateAsync(request)).resolves.toEqual(announcement);
    expect(body).toEqual(request);
  });

  it("puts the update request and resolves the updated announcement from the backend envelope", async () => {
    const update = { title: "Exam week moved", importance: "URGENT" as const };
    const updated = { ...announcement, ...update };
    let body: unknown;
    server.use(
      http.put(apiUrl("/v1/announcements/9"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: updated });
      }),
    );
    const { result } = renderHook(() => useUpdateAnnouncement(), { wrapper });

    await expect(result.current.mutateAsync({ id: 9, data: update })).resolves.toEqual(updated);
    expect(body).toEqual(update);
  });

  it("deletes an announcement and resolves undefined", async () => {
    let deleted = false;
    server.use(
      http.delete(apiUrl("/v1/announcements/9"), () => {
        deleted = true;
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { result } = renderHook(() => useDeleteAnnouncement(), { wrapper });

    await expect(result.current.mutateAsync(9)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
  });
});
