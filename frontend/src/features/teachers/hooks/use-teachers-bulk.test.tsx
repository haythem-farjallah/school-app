import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useBulkDeleteTeachers,
  useBulkEmailTeachers,
  useBulkExportTeachers,
  useBulkUpdateTeacherStatus,
} from "./use-teachers-bulk";
import { apiUrl, server } from "@/test/server";

const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  // A cached list page, so each test can see whether the mutation invalidated it.
  queryClient.setQueryData(["teachers", 0], { content: [] });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const listInvalidated = () => queryClient.getQueryState(["teachers", 0])?.isInvalidated;
  return { wrapper, listInvalidated };
}

describe("teacher bulk operations", () => {
  it("deletes the selected teachers, resolves undefined and refreshes the list", async () => {
    let body: unknown;
    server.use(
      http.delete(apiUrl("/admin/teachers/bulk"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkDeleteTeachers(), { wrapper });

    await expect(result.current.mutateAsync([7, 8])).resolves.toBeUndefined();
    expect(body).toEqual([7, 8]);
    expect(listInvalidated()).toBe(true);
  });

  it("updates the status of the selected teachers, resolves undefined and refreshes the list", async () => {
    let body: unknown;
    server.use(
      http.patch(apiUrl("/admin/teachers/bulk/status"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Teacher statuses updated successfully", data: null });
      }),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkUpdateTeacherStatus(), { wrapper });

    await expect(result.current.mutateAsync({ ids: [7, 8], status: "INACTIVE" })).resolves.toBeUndefined();
    expect(body).toEqual({ ids: [7, 8], status: "INACTIVE" });
    expect(listInvalidated()).toBe(true);
  });

  it("sends the bulk email request and resolves undefined", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/teachers/bulk/email"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Bulk email initiated for 2 teachers", data: null });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useBulkEmailTeachers(), { wrapper });

    await expect(
      result.current.mutateAsync({ ids: [7, 8], subject: "Staff meeting", message: "Friday at 3pm." }),
    ).resolves.toBeUndefined();
    expect(body).toEqual({ ids: [7, 8], subject: "Staff meeting", message: "Friday at 3pm." });
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

    it("exports every teacher as an Excel workbook saved under the server's filename", async () => {
      const workbook = new Uint8Array([0x50, 0x4b, 0x03, 0x04, 0x14]);
      let body: unknown;
      let accept: string | null = null;
      server.use(
        http.post(apiUrl("/admin/teachers/export/excel"), async ({ request }) => {
          body = await request.json();
          accept = request.headers.get("Accept");
          return new HttpResponse(workbook, {
            headers: {
              "Content-Type": XLSX,
              "Content-Disposition": 'attachment; filename="teachers_export_20260926_101500.xlsx"',
            },
          });
        }),
      );
      const { wrapper } = setup();
      const { result } = renderHook(() => useBulkExportTeachers(), { wrapper });

      const exported = await result.current.mutateAsync({ format: "xlsx" });

      expect(body).toEqual({});
      expect(accept).toBe(XLSX);
      expect(exported.filename).toBe("teachers_export_20260926_101500.xlsx");
      expect(new Uint8Array(exported.data as ArrayBuffer)).toEqual(workbook);
      expect(downloads).toHaveLength(1);
      expect(downloads[0].name).toBe("teachers_export_20260926_101500.xlsx");
      expect(downloads[0].blob.type).toBe(XLSX);
      expect(downloads[0].blob.size).toBe(workbook.length);
    });
  });
});
