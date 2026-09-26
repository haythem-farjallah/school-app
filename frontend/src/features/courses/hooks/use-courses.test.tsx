import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCreateCourse, useDeleteCourse, useUpdateCourse } from "./use-courses";
import { api } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";
import type { Course } from "@/types/course";

const course: Course = { id: 7, name: "Physics", color: "#3366ff", credit: 3, weeklyCapacity: 4, teacherId: 2 };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("course mutations", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("creates a course through the api client and resolves the course from the backend envelope", async () => {
    const post = vi.spyOn(api, "post");
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/courses"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: course });
      }),
    );
    const { result } = renderHook(() => useCreateCourse(), { wrapper });
    const newCourse = { name: "Physics", color: "#3366ff", credit: 3, weeklyCapacity: 4, teacherId: 2 };

    await expect(result.current.mutateAsync(newCourse)).resolves.toEqual(course);
    expect(body).toEqual(newCourse);
    expect(post).toHaveBeenCalledOnce();
  });

  it("updates a course through the api client and resolves the course from the backend envelope", async () => {
    const put = vi.spyOn(api, "put");
    const renamed = { ...course, name: "Applied Physics" };
    server.use(
      http.put(apiUrl("/v1/courses/7"), () => HttpResponse.json({ status: "success", data: renamed })),
    );
    const { result } = renderHook(() => useUpdateCourse(), { wrapper });

    await expect(result.current.mutateAsync(renamed)).resolves.toEqual(renamed);
    expect(put).toHaveBeenCalledOnce();
  });

  it("deletes a course through the api client and resolves undefined", async () => {
    const remove = vi.spyOn(api, "delete");
    let deleted = false;
    server.use(
      http.delete(apiUrl("/v1/courses/7"), () => {
        deleted = true;
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { result } = renderHook(() => useDeleteCourse(), { wrapper });

    await expect(result.current.mutateAsync(7)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
    expect(remove).toHaveBeenCalledOnce();
  });
});
