import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { ResourceComments } from "./resource-comments";
import { apiUrl, server } from "@/test/server";

describe("ResourceComments", () => {
  it("shows each comment with the name of its author", async () => {
    const comment = {
      id: 2,
      content: "Very useful",
      resourceId: 8,
      resourceTitle: "Fractions",
      commentedById: 11,
      commentedByName: "Sam Student",
      createdAt: "2026-09-20T10:00:00",
    };
    server.use(
      http.get(apiUrl("/v1/resource-comments/resource/8"), () =>
        HttpResponse.json({ status: "success", data: { content: [comment], page: 0, size: 20, totalElements: 1 } }),
      ),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <ResourceComments resourceId={8} />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Very useful")).toBeInTheDocument();
    expect(screen.getByText("Sam Student")).toBeInTheDocument();
    expect(screen.getByText("Comments (1)")).toBeInTheDocument();
  });
});
