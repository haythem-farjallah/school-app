import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  studentKeys,
  useCreateStudent,
  useDeleteStudent,
  useStudent,
  useStudents,
  useUpdateStudent,
} from "./use-students";
import { apiUrl, server } from "@/test/server";
import type { CreateStudentData } from "@/types/student";
import { pageOf, student } from "../test-utils";

const createData: CreateStudentData = {
  profile: {
    firstName: "Sam",
    lastName: "Student",
    email: "sam@school.test",
    telephone: null,
    birthday: "2010-04-02",
    gender: "M",
    address: null,
  },
  gradeLevel: "HIGH",
  enrollmentYear: 2024,
};

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

describe("useStudents", () => {
  it("lists a page from GET /v1/students when there is no search", async () => {
    let url: URL | undefined;
    server.use(
      http.get(apiUrl("/v1/students"), ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json(pageOf([student], { page: 1, size: 20, totalElements: 41 }));
      }),
    );
    const { wrapper } = setup();

    const { result } = renderHook(() => useStudents({ page: 1, size: 20 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(url?.searchParams.get("page")).toBe("1");
    expect(url?.searchParams.get("size")).toBe("20");
    expect(url?.searchParams.has("search")).toBe(false);
    expect(result.current.data).toEqual({ data: [student], page: 1, totalPages: 3, totalItems: 41 });
  });

  it("searches through GET /v1/students/search?q= and never sends the query to the plain list", async () => {
    let url: URL | undefined;
    let listCalled = false;
    server.use(
      http.get(apiUrl("/v1/students/search"), ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json(pageOf([student]));
      }),
      http.get(apiUrl("/v1/students"), () => {
        listCalled = true;
        return HttpResponse.json(pageOf([]));
      }),
    );
    const { wrapper } = setup();

    const { result } = renderHook(() => useStudents({ page: 0, size: 10, search: "  sam@school " }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(url?.pathname).toMatch(/\/v1\/students\/search$/);
    expect(url?.searchParams.get("q")).toBe("sam@school");
    expect(url?.searchParams.get("page")).toBe("0");
    expect(url?.searchParams.get("size")).toBe("10");
    expect(listCalled).toBe(false);
    expect(result.current.data?.data).toEqual([student]);
  });
});

describe("student hooks", () => {
  it("loads a single student", async () => {
    server.use(http.get(apiUrl("/v1/students/5"), () => HttpResponse.json({ status: "success", data: student })));
    const { wrapper } = setup();

    const { result } = renderHook(() => useStudent(5), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(student);
  });

  it("creates a student with the nested profile body and refreshes the lists", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/students"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: student }, { status: 201 });
      }),
    );
    const { queryClient, wrapper } = setup();
    queryClient.setQueryData(studentKeys.list({ page: 0, size: 10, search: "" }), { data: [] });
    const { result } = renderHook(() => useCreateStudent(), { wrapper });

    await expect(result.current.mutateAsync(createData)).resolves.toEqual(student);
    expect(body).toEqual(createData);
    expect(queryClient.getQueryState(studentKeys.list({ page: 0, size: 10, search: "" }))?.isInvalidated).toBe(true);
  });

  it("patches only the fields the backend update accepts, caches the result and refreshes the lists", async () => {
    const updated = { ...student, firstName: "Samira", gradeLevel: "UNIVERSITY", enrollmentYear: 2025 };
    let body: unknown;
    server.use(
      http.patch(apiUrl("/v1/students/5"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: updated });
      }),
    );
    const { queryClient, wrapper } = setup();
    queryClient.setQueryData(studentKeys.detail(5), student);
    queryClient.setQueryData(studentKeys.list({ page: 0, size: 10, search: "" }), { data: [student] });
    const { result } = renderHook(() => useUpdateStudent(), { wrapper });

    await expect(
      result.current.mutateAsync({ id: 5, firstName: "Samira", lastName: "Student", gradeLevel: "UNIVERSITY", enrollmentYear: 2025 }),
    ).resolves.toEqual(updated);

    expect(body).toEqual({ firstName: "Samira", lastName: "Student", gradeLevel: "UNIVERSITY", enrollmentYear: 2025 });
    expect(queryClient.getQueryData(studentKeys.detail(5))).toEqual(updated);
    expect(queryClient.getQueryState(studentKeys.list({ page: 0, size: 10, search: "" }))?.isInvalidated).toBe(true);
  });

  it("deletes a student, forgets its detail and refreshes the lists", async () => {
    let deleted = false;
    server.use(
      http.delete(apiUrl("/v1/students/5"), () => {
        deleted = true;
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { queryClient, wrapper } = setup();
    queryClient.setQueryData(studentKeys.detail(5), student);
    queryClient.setQueryData(studentKeys.list({ page: 0, size: 10, search: "" }), { data: [student] });
    const { result } = renderHook(() => useDeleteStudent(), { wrapper });

    await expect(result.current.mutateAsync(5)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
    expect(queryClient.getQueryData(studentKeys.detail(5))).toBeUndefined();
    expect(queryClient.getQueryState(studentKeys.list({ page: 0, size: 10, search: "" }))?.isInvalidated).toBe(true);
  });
});
