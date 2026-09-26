import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCreateParent, useDeleteParent, useParent, useUpdateParent } from "./use-parents";
import { apiUrl, server } from "@/test/server";
import type { CreateParentData, Parent } from "@/types/parent";

const parent: Parent = {
  id: 9,
  firstName: "Pat",
  lastName: "Parent",
  email: "pat@family.test",
  telephone: "555-0300",
  birthday: "1980-01-20",
  gender: "F",
  address: "4 Family Road",
  preferredContactMethod: "EMAIL",
  relation: "Mother",
  children: [{ id: 5, firstName: "Sam", lastName: "Student", email: "sam@school.test" }],
};

const parentData: CreateParentData = {
  profile: {
    firstName: "Pat",
    lastName: "Parent",
    email: "pat@family.test",
    telephone: "555-0300",
    birthday: "1980-01-20",
    gender: "F",
    address: "4 Family Road",
  },
  preferredContactMethod: "EMAIL",
  relation: "Mother",
  childrenEmails: ["sam@school.test"],
};

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

describe("parent hooks", () => {
  it("loads a single parent from the backend envelope", async () => {
    server.use(
      http.get(apiUrl("/admin/parent-management/9"), () => HttpResponse.json({ status: "success", data: parent })),
    );
    const { wrapper } = setup();

    const { result } = renderHook(() => useParent(9), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(parent);
  });

  it("creates a parent and resolves the parent from the backend envelope", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/parent-management"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: parent });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useCreateParent(), { wrapper });

    await expect(result.current.mutateAsync(parentData)).resolves.toEqual(parent);
    expect(body).toEqual(parentData);
  });

  it("patches the changed fields without the id, resolves the updated parent and caches it", async () => {
    const updated = { ...parent, preferredContactMethod: "PHONE" as const, telephone: "555-0399" };
    let body: unknown;
    server.use(
      http.patch(apiUrl("/admin/parent-management/9"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: updated });
      }),
    );
    const { queryClient, wrapper } = setup();
    const { result } = renderHook(() => useUpdateParent(), { wrapper });

    await expect(
      result.current.mutateAsync({
        id: 9,
        telephone: "555-0399",
        preferredContactMethod: "PHONE",
        children: ["sam@school.test"],
      }),
    ).resolves.toEqual(updated);
    expect(body).toEqual({ telephone: "555-0399", preferredContactMethod: "PHONE", children: ["sam@school.test"] });
    expect(queryClient.getQueryData(["parent", 9])).toEqual(updated);
  });

  it("deletes a parent and resolves undefined", async () => {
    let deleted = false;
    server.use(
      http.delete(apiUrl("/admin/parent-management/9"), () => {
        deleted = true;
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useDeleteParent(), { wrapper });

    await expect(result.current.mutateAsync(9)).resolves.toBeUndefined();
    expect(deleted).toBe(true);
  });
});
