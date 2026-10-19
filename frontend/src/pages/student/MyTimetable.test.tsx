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
  it("requests only the authenticated timetable and places every slot in its day and actual time row", async () => {
    const requested: string[] = [];
    server.use(http.get(apiUrl("/*"), ({ request }) => {
      const url = new URL(request.url);
      requested.push(url.pathname + url.search);
      return url.pathname === "/api/v1/timetables/me"
        ? HttpResponse.json(studentTimetable)
        : new HttpResponse(null, { status: 500 });
    }));
    renderPage();

    const grid = within(await screen.findByRole("table", { name: "Weekly timetable" }));
    expect(requested).toEqual(["/api/v1/timetables/me"]);
    expect(screen.getByRole("heading", { level: 1, name: "My Timetable" })).toBeInTheDocument();
    expect(grid.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
      "Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
    ]);
    const rows = grid.getAllByRole("row").slice(1);
    expect(rows.map((row) => within(row).getByRole("rowheader").textContent)).toEqual([
      "08:15 – 09:00", "09:15 – 10:00",
    ]);
    const earlyCells = within(rows[0]).getAllByRole("cell");
    expect(earlyCells[0]).toHaveTextContent("Physics");
    const laterCells = within(rows[1]).getAllByRole("cell");
    expect(laterCells[0]).toHaveTextContent("Mathematics");
    expect(laterCells[0]).toHaveTextContent("Period 2");
    expect(laterCells[0]).toHaveTextContent("09:15 – 10:00");
    expect(laterCells[0]).toHaveTextContent("7A");
    expect(laterCells[0]).toHaveTextContent("Nadia Ben Ali");
    expect(laterCells[0]).toHaveTextContent("Room 12");
    expect(laterCells[5]).toHaveTextContent("Art");
    expect(laterCells[6]).toHaveTextContent("History");
    expect(laterCells[1]).toBeEmptyDOMElement();
    expect(screen.queryByText(/Academic year|Semester|Class ID|Free Period/i)).not.toBeInTheDocument();
  });

  it("lets a mobile reader choose any day, including an empty day and Sunday", async () => {
    server.use(http.get(apiUrl("/v1/timetables/me"), () => HttpResponse.json(studentTimetable)));
    renderPage();
    const selector = await screen.findByRole("combobox", { name: "Day" });
    expect(selector).toHaveValue("MONDAY");
    const monday = within(screen.getByRole("region", { name: "Monday" }));
    expect(monday.getAllByRole("listitem").map((slot) => slot.textContent)).toEqual([
      expect.stringContaining("Physics"), expect.stringContaining("Mathematics"),
    ]);
    await userEvent.setup().selectOptions(selector, "TUESDAY");
    const tuesday = within(screen.getByRole("region", { name: "Tuesday" }));
    expect(tuesday.getByText("No lessons scheduled for this day")).toBeInTheDocument();
    expect(tuesday.queryByRole("list")).not.toBeInTheDocument();
    await userEvent.setup().selectOptions(selector, "SUNDAY");
    const sunday = within(screen.getByRole("region", { name: "Sunday" }));
    expect(sunday.getByText("History")).toBeInTheDocument();
    expect(sunday.getByText("09:15 – 10:00")).toBeInTheDocument();
    expect(sunday.queryByText("Mathematics")).not.toBeInTheDocument();
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
    expect(await screen.findByRole("table", { name: "Weekly timetable" })).toBeInTheDocument();
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
    expect(await screen.findByRole("table", { name: "Weekly timetable" })).toBeInTheDocument();
    expect(calls).toBe(2);
  });

  it("retains shared-period slots and omits missing details without inventing time rows", async () => {
    const slot = studentTimetable.data.slots[0];
    server.use(http.get(apiUrl("/v1/timetables/me"), () => HttpResponse.json({
      ...studentTimetable,
      data: { ...studentTimetable.data, slots: [slot,
        { ...slot, id: 99, forCourseName: "Biology", forClassName: "7B" },
        { ...slot, id: 100, forCourseName: "Independent study", forClassName: null, teacherFirstName: null,
          teacherLastName: null, roomName: null, periodIndex: null, periodStartTime: null, periodEndTime: null },
      ] },
    })));
    renderPage();
    const grid = within(await screen.findByRole("table", { name: "Weekly timetable" }));
    const rows = grid.getAllByRole("row").slice(1);
    expect(rows).toHaveLength(2);
    const sharedCell = within(rows[0]).getAllByRole("cell")[0];
    expect(sharedCell).toHaveTextContent("Mathematics");
    expect(sharedCell).toHaveTextContent("Biology");
    expect(within(sharedCell).getAllByRole("listitem")).toHaveLength(2);
    const missingDetailsCell = within(rows[1]).getAllByRole("cell")[0];
    expect(missingDetailsCell).toHaveTextContent(/^Independent study$/);
    expect(within(rows[1]).getByRole("rowheader")).toBeEmptyDOMElement();
    expect(grid.getAllByRole("listitem")).toHaveLength(3);
    expect(screen.queryByText(/Unknown|undefined|Class ID|Period 0/i)).not.toBeInTheDocument();
  });
});
