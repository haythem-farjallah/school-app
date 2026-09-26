import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useBulkDeleteParents } from "./use-parents-bulk";
import { apiUrl, server } from "@/test/server";

describe("useBulkDeleteParents", () => {
  it("deletes each selected parent, resolves undefined and refreshes the list", async () => {
    const deleted: string[] = [];
    server.use(
      http.delete(apiUrl("/admin/parent-management/:id"), ({ params }) => {
        deleted.push(params.id as string);
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    queryClient.setQueryData(["parents", 0], { content: [] });
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useBulkDeleteParents(), { wrapper });

    await expect(result.current.mutateAsync([9, 10])).resolves.toBeUndefined();
    expect(deleted.sort()).toEqual(["10", "9"]);
    expect(queryClient.getQueryState(["parents", 0])?.isInvalidated).toBe(true);
  });
});
