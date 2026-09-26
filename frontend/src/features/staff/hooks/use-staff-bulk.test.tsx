import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useBulkDeleteStaff,
  useBulkEmailStaff,
  useBulkExportStaff,
  useBulkUpdateDepartment,
  useBulkUpdateStaffStatus,
  useBulkUpdateStaffType,
} from "./use-staff-bulk";
import { apiUrl, server } from "@/test/server";

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  // A cached list page, so each test can see whether the mutation invalidated it.
  queryClient.setQueryData(["staff", 0], { content: [] });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const listInvalidated = () => queryClient.getQueryState(["staff", 0])?.isInvalidated;
  return { wrapper, listInvalidated };
}

// Records the body of each PATCH /admin/staff/{id}, keyed by id.
function recordStaffPatches() {
  const patches: Record<string, unknown> = {};
  server.use(
    http.patch(apiUrl("/admin/staff/:id"), async ({ params, request }) => {
      patches[params.id as string] = await request.json();
      return HttpResponse.json({ status: "Staff member updated successfully", data: { id: Number(params.id) } });
    }),
  );
  return patches;
}

describe("staff bulk operations", () => {
  it("deletes the selected staff members, resolves undefined and refreshes the list", async () => {
    let body: unknown;
    server.use(
      http.delete(apiUrl("/admin/staff/bulk"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkDeleteStaff(), { wrapper });

    await expect(result.current.mutateAsync([12, 13])).resolves.toBeUndefined();
    expect(body).toEqual([12, 13]);
    expect(listInvalidated()).toBe(true);
  });

  it("updates the status of the selected staff members, resolves undefined and refreshes the list", async () => {
    let body: unknown;
    server.use(
      http.patch(apiUrl("/admin/staff/bulk/status"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Staff statuses updated successfully", data: null });
      }),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkUpdateStaffStatus(), { wrapper });

    await expect(result.current.mutateAsync({ ids: [12, 13], status: "ACTIVE" })).resolves.toBeUndefined();
    expect(body).toEqual({ ids: [12, 13], status: "ACTIVE" });
    expect(listInvalidated()).toBe(true);
  });

  it("patches only the department of each selected staff member and resolves undefined", async () => {
    const patches = recordStaffPatches();
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkUpdateDepartment(), { wrapper });

    await expect(result.current.mutateAsync({ ids: [12, 13], department: "IT" })).resolves.toBeUndefined();
    expect(patches).toEqual({ "12": { department: "IT" }, "13": { department: "IT" } });
    expect(listInvalidated()).toBe(true);
  });

  it("patches only the staff type of each selected staff member and resolves undefined", async () => {
    const patches = recordStaffPatches();
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkUpdateStaffType(), { wrapper });

    await expect(result.current.mutateAsync({ ids: [12, 13], staffType: "SECURITY" })).resolves.toBeUndefined();
    expect(patches).toEqual({ "12": { staffType: "SECURITY" }, "13": { staffType: "SECURITY" } });
    expect(listInvalidated()).toBe(true);
  });

  it("sends the bulk email request and resolves undefined", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/staff/bulk/email"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Bulk email initiated for 2 staff members", data: null });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useBulkEmailStaff(), { wrapper });
    const email = { ids: [12, 13], subject: "Fire drill", message: "Tuesday at 10am.", actionText: "Plan" };

    await expect(result.current.mutateAsync(email)).resolves.toBeUndefined();
    expect(body).toEqual(email);
  });

  describe("export", () => {
    let downloads: { name: string; blob: Blob }[];

    // jsdom has no object URLs and does not download on anchor clicks, so record what would be saved.
    beforeEach(() => {
      downloads = [];
      const blobs = new Map<string, Blob>();
      URL.createObjectURL = (blob: Blob) => {
        const url = `blob:test/${blobs.size}`;
        blobs.set(url, blob as Blob);
        return url;
      };
      URL.revokeObjectURL = () => {};
      vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (this: HTMLAnchorElement) {
        downloads.push({ name: this.download, blob: blobs.get(this.href)! });
      });
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it("posts the selected ids and saves the CSV body under the server's filename", async () => {
      const csv = "id,firstName\n12,Sara\n";
      let body: unknown;
      server.use(
        http.post(apiUrl("/admin/staff/export/csv"), async ({ request }) => {
          body = await request.json();
          return new HttpResponse(csv, {
            headers: {
              "Content-Type": "text/csv",
              "Content-Disposition": 'attachment; filename="staff_export_20260926_101500.csv"',
            },
          });
        }),
      );
      const { wrapper } = setup();
      const { result } = renderHook(() => useBulkExportStaff(), { wrapper });

      await expect(result.current.mutateAsync({ ids: [12], format: "csv" })).resolves.toEqual({
        data: csv,
        filename: "staff_export_20260926_101500.csv",
      });
      expect(body).toEqual({ ids: [12] });
      expect(downloads.map((download) => download.name)).toEqual(["staff_export_20260926_101500.csv"]);
      expect(downloads[0].blob.size).toBe(csv.length);
    });
  });
});
