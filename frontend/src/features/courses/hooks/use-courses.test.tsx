import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCourses, useCreateCourse, useDeleteCourse, useUpdateCourse } from "./use-courses";
import { api } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";
import type { Course } from "@/types/course";

const course: Course = { id: 7, name: "Physics", color: "#3366ff", credit: 3, weeklyCapacity: 4, teacherId: 2 };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
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

describe("useCourses filter contract", () => {
  it.each([
    [{ name: "  Math ", credit: "3", weeklyCapacity: "4", teacherId: "123", search: "  math  " }, { name_like: "Math", credit_eq: "3", weeklyCapacity_eq: "4", "teacher.id_eq": "123", search: "math" }],
    [{ credit: 3.5, weeklyCapacity: 4, teacherId: 123 }, { credit_eq: "3.5", weeklyCapacity_eq: "4", "teacher.id_eq": "123" }],
    [{ "school.id": 99, "teacher.password": "secret", include: "teacher", "fields[course]": "id", sort: "school.id:asc" }, {}],
    [{ name: " ", credit: undefined, weeklyCapacity: null, teacherId: "", search: " " }, {}],
  ])("sends only canonical parameters and preserves pagination: %j", async (filters, expected) => {
    let url: URL | undefined;
    server.use(
      http.get(apiUrl("/v1/courses/filter"), ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json({ status: "success", data: { content: [course], page: 1, size: 2, totalElements: 5 } });
      }),
    );
    const { result } = renderHook(() => useCourses({ page: 1, size: 2, ...filters }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(Object.fromEntries(url!.searchParams)).toEqual({ page: "1", size: "2", ...expected });
    expect(result.current.data).toEqual({ data: [course], page: 1, totalPages: 3, totalItems: 5 });
  });
});
