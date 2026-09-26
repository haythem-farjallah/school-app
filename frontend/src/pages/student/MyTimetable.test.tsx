import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import MyTimetable from "./MyTimetable";
import { apiUrl, server } from "@/test/server";

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MyTimetable />
    </QueryClientProvider>,
  );
}

describe("student MyTimetable", () => {
  it("shows the timetable of the class the student is enrolled in", async () => {
    server.use(
      http.get(apiUrl("/v1/dashboard/current-user"), () =>
        HttpResponse.json({ status: "success", data: { type: "STUDENT", enrolledClasses: [{ classId: 4, className: "7A" }] } }),
      ),
      http.get(apiUrl("/v1/periods"), () => HttpResponse.json([])),
      http.get(apiUrl("/v1/timetables/class/4"), () => HttpResponse.json({ status: "success", data: { id: 2, slots: [] } })),
    );
    renderPage();

    expect(await screen.findByText("Class: 7A")).toBeInTheDocument();
  });

  it("says there is no class information when the student has no enrolled class, without showing another class", async () => {
    // No /v1/classes handler: asking for any other class would fail the test.
    server.use(
      http.get(apiUrl("/v1/dashboard/current-user"), () =>
        HttpResponse.json({ status: "success", data: { type: "STUDENT", enrolledClasses: [] } }),
      ),
    );
    renderPage();

    expect(await screen.findByText("No Class Information")).toBeInTheDocument();
  });
});
