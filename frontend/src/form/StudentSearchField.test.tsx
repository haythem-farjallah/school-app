import { beforeAll, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { FormProvider, useForm } from "react-hook-form";
import { http, HttpResponse } from "msw";
import { api } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";
import type { Student } from "@/types/parent";
import { StudentSearchField } from "./StudentSearchField";

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

function TestForm() {
  const methods = useForm<{ students: Student[] }>({ defaultValues: { students: [] } });

  return (
    <FormProvider {...methods}>
      <StudentSearchField
        field={{
          name: "students",
          type: "student-search",
          label: "Students",
          placeholder: "Search students...",
        }}
      />
    </FormProvider>
  );
}

describe("StudentSearchField", () => {
  it("searches after two characters and keeps the selected student", async () => {
    const get = vi.spyOn(api, "get");
    let listCalls = 0;
    let searchCalls = 0;
    server.use(
      http.get(apiUrl("/v1/students"), () => {
        listCalls++;
        return HttpResponse.json({
          status: "success",
          data: { content: [], page: 0, size: 1, totalElements: 0 },
        });
      }),
      http.get(apiUrl("/v1/students/search"), ({ request }) => {
        searchCalls++;
        expect(new URL(request.url).searchParams.get("q")).toBe("Sa");
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
        <TestForm />
      </QueryClientProvider>,
    );
    const user = userEvent.setup();

    await user.click(screen.getByRole("combobox"));
    const input = screen.getByPlaceholderText("Search by name or email (min 2 chars)...");
    await user.type(input, "S");
    expect(searchCalls).toBe(0);

    await user.type(input, "a");
    await user.click(await screen.findByText("sam@school.test"));

    expect(screen.getByText("1 student(s) selected")).toBeInTheDocument();
    expect(listCalls).toBe(0);
    expect(searchCalls).toBe(1);
    expect(get).toHaveBeenCalledOnce();
  });
});
