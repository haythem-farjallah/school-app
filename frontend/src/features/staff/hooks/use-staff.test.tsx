import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCreateStaff, useDeleteStaff, useStaffById, useUpdateStaff } from "./use-staff";
import { apiUrl, server } from "@/test/server";
import { StaffType, type CreateStaffRequest, type Staff, type UpdateStaffRequest } from "@/types/staff";

const staff: Staff = {
  id: 12,
  firstName: "Sara",
  lastName: "Staff",
  email: "sara@school.test",
  telephone: "555-0200",
  birthday: "1985-06-15",
  gender: "F",
  address: "3 Office Lane",
  staffType: StaffType.ADMINISTRATIVE,
  department: "FINANCE",
  createdAt: "2026-01-10T08:00:00Z",
  updatedAt: "2026-01-10T08:00:00Z",
};

const createRequest: CreateStaffRequest = {
  profile: {
    firstName: "Sara",
    lastName: "Staff",
    email: "sara@school.test",
    telephone: "555-0200",
    birthday: "1985-06-15",
    gender: "F",
    address: "3 Office Lane",
    role: "STAFF",
  },
  staffType: StaffType.ADMINISTRATIVE,
  department: "FINANCE",
};

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

describe("staff hooks", () => {
  it("loads a single staff member from the backend envelope", async () => {
    server.use(
      http.get(apiUrl("/admin/staff/12"), () =>
        HttpResponse.json({ status: "Staff member retrieved successfully", data: staff }),
      ),
    );
    const { wrapper } = setup();

    const { result } = renderHook(() => useStaffById(12), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(staff);
  });

  it("creates a staff member with the nested profile and resolves the staff member from the backend envelope", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/staff"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Staff member created successfully", data: staff }, { status: 201 });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useCreateStaff(), { wrapper });

    await expect(result.current.mutateAsync(createRequest)).resolves.toEqual(staff);
    expect(body).toEqual(createRequest);
  });

  it("patches the update request, resolves the updated staff member and caches it", async () => {
    const updated = { ...staff, department: "IT", address: "4 Server Road" };
    const update: UpdateStaffRequest = { profile: { address: "4 Server Road" }, department: "IT" };
    let body: unknown;
    server.use(
      http.patch(apiUrl("/admin/staff/12"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Staff member updated successfully", data: updated });
      }),
    );
    const { queryClient, wrapper } = setup();
    const { result } = renderHook(() => useUpdateStaff(), { wrapper });

    await expect(result.current.mutateAsync({ id: 12, data: update })).resolves.toEqual(updated);
    expect(body).toEqual(update);
    expect(queryClient.getQueryData(["staff", 12])).toEqual(updated);
  });

  it("deletes a staff member and resolves undefined", async () => {
    let deleted = false;
    server.use(
      http.delete(apiUrl("/admin/staff/12"), () => {
        deleted = true;
        return HttpResponse.json({ status: "Staff member deleted successfully", data: null });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useDeleteStaff(), { wrapper });

    await expect(result.current.mutateAsync(12)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
  });
});
