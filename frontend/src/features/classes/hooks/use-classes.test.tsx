import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useClass, useCreateClass, useDeleteClass, useUpdateClass } from "./use-classes";
import { apiUrl, server } from "@/test/server";
import type { Class, CreateClassRequest, UpdateClassRequest } from "@/types/class";

const schoolClass: Class = {
  id: 4,
  name: "10-A",
  yearOfStudy: 10,
  maxStudents: 30,
  studentIds: [21, 22],
  courseIds: [7],
  teacherIds: [3],
  assignedRoomId: null,
};

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("class hooks", () => {
  it("loads a single class from the backend envelope", async () => {
    server.use(
      http.get(apiUrl("/v1/classes/4"), () => HttpResponse.json({ status: "success", data: schoolClass })),
    );

    const { result } = renderHook(() => useClass(4), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(schoolClass);
  });

  it("posts the create request and resolves the class from the backend envelope", async () => {
    const request: CreateClassRequest = { name: "10-A", yearOfStudy: 10, maxStudents: 30 };
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/classes"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: schoolClass });
      }),
    );
    const { result } = renderHook(() => useCreateClass(), { wrapper });

    await expect(result.current.mutateAsync(request)).resolves.toEqual(schoolClass);
    expect(body).toEqual(request);
  });

  it("puts the update request and resolves the updated class from the backend envelope", async () => {
    const update: UpdateClassRequest = { name: "10-B", yearOfStudy: 10, maxStudents: 28 };
    const updated = { ...schoolClass, ...update };
    let body: unknown;
    server.use(
      http.put(apiUrl("/v1/classes/4"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: updated });
      }),
    );
    const { result } = renderHook(() => useUpdateClass(), { wrapper });

    await expect(result.current.mutateAsync({ id: 4, data: update })).resolves.toEqual(updated);
    expect(body).toEqual(update);
  });

  it("deletes a class and resolves undefined", async () => {
    let deleted = false;
    server.use(
      http.delete(apiUrl("/v1/classes/4"), () => {
        deleted = true;
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { result } = renderHook(() => useDeleteClass(), { wrapper });

    await expect(result.current.mutateAsync(4)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
  });
});
