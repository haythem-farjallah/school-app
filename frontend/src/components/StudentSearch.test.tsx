import { beforeAll, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { StudentSearch } from "./StudentSearch";
import { apiUrl, server } from "@/test/server";

// The command menu measures its list; jsdom has neither API.
beforeAll(() => {
  vi.stubGlobal(
    "ResizeObserver",
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  );
  Element.prototype.scrollIntoView = () => {};
});

describe("StudentSearch", () => {
  it("lists the students returned by the search endpoint", async () => {
    let query: string | null = null;
    server.use(
      http.get(apiUrl("/v1/students/search"), ({ request }) => {
        query = new URL(request.url).searchParams.get("q");
        return HttpResponse.json({
          status: "success",
          data: {
            content: [{ id: 3, firstName: "Sam", lastName: "Student", email: "sam@school.test" }],
            page: 0,
            size: 10,
            totalElements: 1,
          },
        });
      }),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <StudentSearch selectedStudents={[]} onStudentsChange={() => {}} />
      </QueryClientProvider>,
    );
    const user = userEvent.setup();

    await user.click(screen.getByRole("combobox"));
    await user.type(screen.getByPlaceholderText("Search by name or email..."), "Sam");

    expect(await screen.findByText("sam@school.test")).toBeInTheDocument();
    expect(query).toBe("Sam");
  });
});
