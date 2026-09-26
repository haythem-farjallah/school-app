import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCreateTeacher, useDeleteTeacher, useTeacher, useUpdateTeacher } from "./use-teachers";
import { apiUrl, server } from "@/test/server";
import type { CreateTeacherData, Teacher } from "@/types/teacher";

const teacher: Teacher = {
  id: 8,
  firstName: "Theo",
  lastName: "Teacher",
  email: "theo@school.test",
  telephone: "555-0200",
  birthday: "1985-09-12",
  gender: "M",
  address: "3 Staff Lane",
  qualifications: "MSc Physics",
  subjectsTaught: "Physics",
  availableHours: 20,
  schedulePreferences: "Mornings",
};

const teacherData: CreateTeacherData = {
  profile: {
    firstName: "Theo",
    lastName: "Teacher",
    email: "theo@school.test",
    telephone: "555-0200",
    birthday: "1985-09-12",
    gender: "M",
    address: "3 Staff Lane",
  },
  qualifications: "MSc Physics",
  subjectsTaught: "Physics",
  availableHours: 20,
  schedulePreferences: "Mornings",
};

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

describe("teacher hooks", () => {
  it("loads a single teacher from the backend envelope", async () => {
    server.use(http.get(apiUrl("/admin/teachers/8"), () => HttpResponse.json({ status: "success", data: teacher })));
    const { wrapper } = setup();

    const { result } = renderHook(() => useTeacher(8), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(teacher);
  });

  it("creates a teacher and resolves the teacher from the backend envelope", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/teachers"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: teacher });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useCreateTeacher(), { wrapper });

    await expect(result.current.mutateAsync(teacherData)).resolves.toEqual(teacher);
    expect(body).toEqual(teacherData);
  });

  it("patches the changed fields without the id, resolves the updated teacher and caches it", async () => {
    const updated = { ...teacher, availableHours: 16, subjectsTaught: "Physics, Maths" };
    let body: unknown;
    server.use(
      http.patch(apiUrl("/admin/teachers/8"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: updated });
      }),
    );
    const { queryClient, wrapper } = setup();
    const { result } = renderHook(() => useUpdateTeacher(), { wrapper });

    await expect(
      result.current.mutateAsync({ id: 8, availableHours: 16, subjectsTaught: "Physics, Maths" }),
    ).resolves.toEqual(updated);
    expect(body).toEqual({ availableHours: 16, subjectsTaught: "Physics, Maths" });
    expect(queryClient.getQueryData(["teacher", 8])).toEqual(updated);
  });

  it("deletes a teacher and resolves undefined", async () => {
    let deleted = false;
    server.use(
      http.delete(apiUrl("/admin/teachers/8"), () => {
        deleted = true;
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useDeleteTeacher(), { wrapper });

    await expect(result.current.mutateAsync(8)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
  });
});
