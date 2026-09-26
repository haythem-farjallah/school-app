import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCreateStudent, useDeleteStudent, useStudent, useUpdateStudent } from "./use-students";
import { apiUrl, server } from "@/test/server";
import type { CreateStudentData, Student } from "@/types/student";

const student: Student = {
  id: 5,
  firstName: "Sam",
  lastName: "Student",
  email: "sam@school.test",
  telephone: "555-0100",
  birthday: "2010-04-02",
  gender: "M",
  address: "1 School Road",
  gradeLevel: "GRADE_9",
  enrollmentYear: 2024,
};

const studentData: CreateStudentData = {
  profile: {
    firstName: "Sam",
    lastName: "Student",
    email: "sam@school.test",
    telephone: "555-0100",
    birthday: "2010-04-02",
    gender: "M",
    address: "1 School Road",
  },
  gradeLevel: "GRADE_9",
  enrollmentYear: 2024,
};

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

describe("student hooks", () => {
  it("loads a single student from the backend envelope", async () => {
    server.use(http.get(apiUrl("/v1/students/5"), () => HttpResponse.json({ status: "success", data: student })));
    const { wrapper } = setup();

    const { result } = renderHook(() => useStudent(5), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(student);
  });

  it("creates a student and resolves the student from the backend envelope", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/students"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: student });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useCreateStudent(), { wrapper });

    await expect(result.current.mutateAsync(studentData)).resolves.toEqual(student);
    expect(body).toEqual(studentData);
  });

  it("patches the flattened profile, resolves the updated student and caches it", async () => {
    const updated = { ...student, address: "2 College Street" };
    let body: unknown;
    server.use(
      http.patch(apiUrl("/v1/students/5"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: updated });
      }),
    );
    const { queryClient, wrapper } = setup();
    const { result } = renderHook(() => useUpdateStudent(), { wrapper });

    await expect(
      result.current.mutateAsync({ ...studentData, profile: { ...studentData.profile, address: "2 College Street" }, id: 5 }),
    ).resolves.toEqual(updated);
    expect(body).toEqual({
      firstName: "Sam",
      lastName: "Student",
      email: "sam@school.test",
      telephone: "555-0100",
      birthday: "2010-04-02",
      gender: "M",
      address: "2 College Street",
      gradeLevel: "GRADE_9",
      enrollmentYear: 2024,
    });
    expect(queryClient.getQueryData(["student", 5])).toEqual(updated);
  });

  it("deletes a student and resolves undefined", async () => {
    let deleted = false;
    server.use(
      http.delete(apiUrl("/v1/students/5"), () => {
        deleted = true;
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useDeleteStudent(), { wrapper });

    await expect(result.current.mutateAsync(5)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
  });
});
