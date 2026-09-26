import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import {
  useDeleteLearningResource,
  useDownloadResource,
  usePreviewResource,
  useUpdateLearningResource,
  useUploadLearningResource,
} from "./use-learning-resources";
import { api } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";
import { ResourceType } from "@/types/learning-resource";

const resource = { id: 8, title: "Fractions", description: "Worksheet", type: "DOCUMENT", isPublic: true };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("learning resource hooks", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  // jsdom's XMLHttpRequest never completes a multipart request under MSW, so this test
  // stops at the api client and checks the request it is given.
  it("uploads the file as multipart form data and resolves the created resource", async () => {
    const post = vi.spyOn(api, "post").mockResolvedValue({ data: { status: "success", data: resource } });
    const { result } = renderHook(() => useUploadLearningResource(), { wrapper });
    const file = new File(["%PDF"], "fractions.pdf", { type: "application/pdf" });

    await expect(
      result.current.mutateAsync({ file, title: "Fractions", description: "Worksheet", type: ResourceType.DOCUMENT, isPublic: true, classIds: [4] }),
    ).resolves.toEqual(resource);
    const [url, form] = post.mock.calls[0] as [string, FormData];
    expect(url).toBe("/v1/learning-resources/upload");
    expect(form.get("title")).toBe("Fractions");
    expect(form.get("classIds")).toBe("[4]");
    expect(form.get("file")).toBe(file);
  });

  it("updates a resource and resolves it from the envelope", async () => {
    let body: unknown;
    server.use(
      http.put(apiUrl("/v1/learning-resources/8"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: { ...resource, title: "Fractions II" } });
      }),
    );
    const { result } = renderHook(() => useUpdateLearningResource(), { wrapper });

    await expect(result.current.mutateAsync({ id: 8, data: { title: "Fractions II" } })).resolves.toEqual({
      ...resource,
      title: "Fractions II",
    });
    expect(body).toEqual({ title: "Fractions II" });
  });

  it("deletes a resource and resolves undefined", async () => {
    server.use(
      http.delete(apiUrl("/v1/learning-resources/8"), () => HttpResponse.json({ status: "success", data: null })),
    );
    const { result } = renderHook(() => useDeleteLearningResource(), { wrapper });

    await expect(result.current.mutateAsync(8)).resolves.toBeUndefined();
  });

  it("downloads and previews the stored file as a blob", async () => {
    server.use(
      http.get(apiUrl("/v1/learning-resources/files/fractions.pdf"), () =>
        new HttpResponse("download-bytes", { headers: { "Content-Type": "application/pdf" } }),
      ),
      http.get(apiUrl("/v1/learning-resources/preview/fractions.pdf"), () =>
        new HttpResponse("preview-bytes", { headers: { "Content-Type": "application/pdf" } }),
      ),
    );
    const download = renderHook(() => useDownloadResource(), { wrapper }).result;
    const preview = renderHook(() => usePreviewResource(), { wrapper }).result;

    const downloaded = await download.current.mutateAsync("fractions.pdf");
    const previewed = await preview.current.mutateAsync("fractions.pdf");

    expect(downloaded).toBeInstanceOf(Blob);
    expect(downloaded.size).toBe("download-bytes".length);
    expect(previewed).toBeInstanceOf(Blob);
    expect(previewed.size).toBe("preview-bytes".length);
  });
});
