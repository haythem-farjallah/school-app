import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useBulkDeleteStudents,
  useBulkEmailStudents,
  useBulkExportStudents,
  useBulkUpdateStudentStatus,
} from "./use-students-bulk";
import { apiUrl, server } from "@/test/server";

// jsdom's Blob has no text(); FileReader reads it.
function readText(blob: Blob) {
  return new Promise<string>((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.readAsText(blob);
  });
}

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  // A cached list page, so each test can see whether the mutation invalidated it.
  queryClient.setQueryData(["students", 0], { content: [] });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const listInvalidated = () => queryClient.getQueryState(["students", 0])?.isInvalidated;
  return { wrapper, listInvalidated };
}

describe("student bulk operations", () => {
  it("deletes the selected students, resolves undefined and refreshes the list", async () => {
    let body: unknown;
    server.use(
      http.delete(apiUrl("/v1/students/bulk"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkDeleteStudents(), { wrapper });

    await expect(result.current.mutateAsync([21, 22])).resolves.toBeUndefined();
    expect(body).toEqual([21, 22]);
    expect(listInvalidated()).toBe(true);
  });

  it("updates the status of the selected students, resolves undefined and refreshes the list", async () => {
    let body: unknown;
    server.use(
      http.patch(apiUrl("/v1/students/bulk/status"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Student statuses updated successfully", data: null });
      }),
    );
    const { wrapper, listInvalidated } = setup();
    const { result } = renderHook(() => useBulkUpdateStudentStatus(), { wrapper });

    await expect(
      result.current.mutateAsync({ ids: [21, 22], status: "SUSPENDED", reason: "Unpaid fees" }),
    ).resolves.toBeUndefined();
    expect(body).toEqual({ ids: [21, 22], status: "SUSPENDED", reason: "Unpaid fees" });
    expect(listInvalidated()).toBe(true);
  });

  it("sends the bulk email request and resolves undefined", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/students/bulk/email"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Bulk email initiated for 2 students", data: null });
      }),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useBulkEmailStudents(), { wrapper });
    const email = {
      ids: [21, 22],
      subject: "Trip",
      message: "Bring a packed lunch.",
      actionUrl: "https://school.test/trip",
      actionText: "Details",
    };

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
      const csv = "id,firstName\n21,Sam\n";
      let body: unknown;
      let accept: string | null = null;
      server.use(
        http.post(apiUrl("/v1/students/export/csv"), async ({ request }) => {
          body = await request.json();
          accept = request.headers.get("Accept");
          return new HttpResponse(csv, {
            headers: {
              "Content-Type": "text/csv",
              "Content-Disposition": 'attachment; filename="students_export_20260926_101500.csv"',
            },
          });
        }),
      );
      const { wrapper } = setup();
      const { result } = renderHook(() => useBulkExportStudents(), { wrapper });

      await expect(result.current.mutateAsync({ ids: [21], format: "csv" })).resolves.toEqual({
        data: csv,
        filename: "students_export_20260926_101500.csv",
      });
      expect(body).toEqual({ ids: [21] });
      expect(accept).toBe("text/csv");
      expect(downloads).toHaveLength(1);
      expect(downloads[0].name).toBe("students_export_20260926_101500.csv");
      expect(await readText(downloads[0].blob)).toBe(csv);
    });

    it("falls back to a dated filename when the server filename is not readable", async () => {
      server.use(
        http.post(apiUrl("/v1/students/export/csv"), () =>
          new HttpResponse("id\n", { headers: { "Content-Type": "text/csv" } }),
        ),
      );
      const { wrapper } = setup();
      const { result } = renderHook(() => useBulkExportStudents(), { wrapper });

      const exported = await result.current.mutateAsync({ format: "csv" });

      expect(exported.filename).toMatch(/^students_export_\d{4}-\d{2}-\d{2}\.csv$/);
      expect(downloads[0].name).toBe(exported.filename);
    });
  });
});
