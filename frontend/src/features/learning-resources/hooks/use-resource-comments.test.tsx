import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { useCreateResourceComment, useDeleteResourceComment, useResourceComments } from "./use-resource-comments";
import { apiUrl, server } from "@/test/server";

const comment = {
  id: 2,
  content: "Very useful",
  resourceId: 8,
  resourceTitle: "Fractions",
  commentedById: 11,
  commentedByName: "Sam Student",
  createdAt: "2026-09-20T10:00:00",
};

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

describe("resource comment hooks", () => {
  it("reads the comment page of a resource from /v1/resource-comments", async () => {
    server.use(
      http.get(apiUrl("/v1/resource-comments/resource/8"), () =>
        HttpResponse.json({ status: "success", data: { content: [comment], page: 0, size: 20, totalElements: 1 } }),
      ),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useResourceComments(8), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content).toEqual([comment]);
  });

  it("creates a comment, resolves it and refreshes that resource's comments", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/resource-comments"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: comment });
      }),
    );
    const { queryClient, wrapper } = setup();
    queryClient.setQueryData(["resource-comments", 8], { content: [] });
    const { result } = renderHook(() => useCreateResourceComment(), { wrapper });

    await expect(result.current.mutateAsync({ content: "Very useful", resourceId: 8 })).resolves.toEqual(comment);
    expect(body).toEqual({ content: "Very useful", resourceId: 8 });
    expect(queryClient.getQueryState(["resource-comments", 8])?.isInvalidated).toBe(true);
  });

  it("deletes a comment and resolves undefined", async () => {
    server.use(
      http.delete(apiUrl("/v1/resource-comments/2"), () => HttpResponse.json({ status: "success", data: null })),
    );
    const { wrapper } = setup();
    const { result } = renderHook(() => useDeleteResourceComment(), { wrapper });

    await expect(result.current.mutateAsync(2)).resolves.toBeUndefined();
  });
});
