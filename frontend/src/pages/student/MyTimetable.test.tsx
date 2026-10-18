import { describe, expect, it } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { I18nextProvider } from "react-i18next";
import { http, HttpResponse } from "msw";
import MyTimetable from "./MyTimetable";
import { apiUrl, server } from "@/test/server";
import { createTestI18n } from "@/test/i18n";
import { studentTimetable } from "@/test/fixtures/student-timetable";

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <I18nextProvider i18n={createTestI18n()}>
      <QueryClientProvider client={queryClient}>
        <MyTimetable />
      </QueryClientProvider>
    </I18nextProvider>,
  );
}

describe("student MyTimetable", () => {
  it("requests only the authenticated timetable and renders flattened slots, including both weekend days", async () => {
    const requested: string[] = [];
    server.use(http.get(apiUrl("/*"), ({ request }) => {
      const url = new URL(request.url);
      requested.push(url.pathname + url.search);
      return url.pathname === "/api/v1/timetables/me"
        ? HttpResponse.json(studentTimetable)
        : new HttpResponse(null, { status: 500 });
    }));
    renderPage();

    expect(await screen.findByText("Mathematics")).toBeInTheDocument();
    expect(requested).toEqual(["/api/v1/timetables/me"]);
    expect(screen.getByRole("heading", { level: 1, name: "My Timetable" })).toBeInTheDocument();
    const monday = within(screen.getByRole("region", { name: "Monday" }));
    expect(monday.getAllByRole("listitem").map((item) => item.textContent)).toEqual([
      expect.stringContaining("Physics"), expect.stringContaining("Mathematics"),
    ]);
    expect(monday.getByText("Period 2")).toBeInTheDocument();
    expect(monday.getByText("09:15 – 10:00")).toBeInTheDocument();
    expect(monday.getByText("7A")).toBeInTheDocument();
    expect(monday.getByText("Nadia Ben Ali")).toBeInTheDocument();
    expect(monday.getByText("Room 12")).toBeInTheDocument();
    expect(within(screen.getByRole("region", { name: "Saturday" })).getByText("Art")).toBeInTheDocument();
    expect(within(screen.getByRole("region", { name: "Sunday" })).getByText("History")).toBeInTheDocument();
    expect(screen.getAllByRole("region").map((region) => region.getAttribute("aria-labelledby"))).toEqual([
      "timetable-MONDAY", "timetable-SATURDAY", "timetable-SUNDAY",
    ]);
    expect(screen.queryByText(/Academic year|Semester|Class ID|Free Period/i)).not.toBeInTheDocument();
  });

  it("shows a loading state until the response arrives", async () => {
    let respond!: () => void;
    const pending = new Promise<void>((resolve) => { respond = resolve; });
    server.use(http.get(apiUrl("/v1/timetables/me"), async () => {
      await pending;
      return HttpResponse.json(studentTimetable);
    }));
    renderPage();
    expect(screen.getByRole("status")).toHaveTextContent("Loading your timetable");
    expect(screen.queryByText("No timetable slots to display")).not.toBeInTheDocument();
    respond();
    expect(await screen.findByText("Mathematics")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows a neutral empty state only for a successful empty slots array", async () => {
    server.use(http.get(apiUrl("/v1/timetables/me"), () =>
      HttpResponse.json({ ...studentTimetable, data: { ...studentTimetable.data, slots: [] } }),
    ));
    renderPage();
    expect(await screen.findByText("No timetable slots to display")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.queryByText(/enroll|administrator|No Class Information/i)).not.toBeInTheDocument();
  });

  it.each([401, 403, 404, 500])("shows an error, never an empty schedule, for HTTP %s", async (status) => {
    server.use(http.get(apiUrl("/v1/timetables/me"), () => HttpResponse.json({
      type: "about:blank", title: "Request failed", status, detail: "Internal details must not be displayed",
    }, { status })));
    renderPage();
    expect(await screen.findByRole("alert")).toHaveTextContent("Your timetable could not be loaded");
    expect(screen.queryByText("No timetable slots to display")).not.toBeInTheDocument();
    expect(screen.queryByText(/Internal details/)).not.toBeInTheDocument();
  });

  it("shows a network failure and retries only when the student requests it", async () => {
    let calls = 0;
    server.use(http.get(apiUrl("/v1/timetables/me"), () => {
      calls++;
      return calls === 1 ? HttpResponse.error() : HttpResponse.json(studentTimetable);
    }));
    renderPage();
    expect(await screen.findByRole("alert")).toHaveTextContent("Your timetable could not be loaded");
    expect(calls).toBe(1);
    expect(screen.queryByText("No timetable slots to display")).not.toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole("button", { name: "Try again" }));
    expect(await screen.findByText("Mathematics")).toBeInTheDocument();
    expect(calls).toBe(2);
  });

  it("does not invent missing slot details or discard slots sharing a period", async () => {
    const slot = studentTimetable.data.slots[0];
    server.use(http.get(apiUrl("/v1/timetables/me"), () => HttpResponse.json({
      ...studentTimetable,
      data: { ...studentTimetable.data, slots: [slot,
        { ...slot, id: 99, forCourseName: "Biology", forClassName: null, teacherFirstName: null,
          teacherLastName: null, roomName: null, periodIndex: null, periodStartTime: null, periodEndTime: null },
      ] },
    })));
    renderPage();
    expect(await screen.findByText("Biology")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getAllByText("Nadia Ben Ali")).toHaveLength(1);
    expect(screen.queryByText(/Unknown|undefined|Class ID|Period 0/i)).not.toBeInTheDocument();
  });
});
