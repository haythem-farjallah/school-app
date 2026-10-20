import { act, cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { QueryClientProvider } from "@tanstack/react-query";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { MemoryRouter, useLocation } from "react-router-dom";
import { delay, http, HttpResponse } from "msw";
import type { GenericAbortSignal } from "axios";
import { api } from "@/lib/api-client";
import StudentAttendance from "./Attendance";
import { queryClient } from "@/lib/query-client";
import { setSessionEndedHandler, terminateSession } from "@/lib/session";
import { token } from "@/lib/token";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";
import { apiUrl, server } from "@/test/server";

const alice = { id: 21, firstName: "Alice", lastName: "Student", email: "alice@fixtures.school.test", role: "STUDENT" };
const bob = { ...alice, id: 22, firstName: "Bob" };
const statuses = ["PRESENT", "ABSENT", "LATE", "EXCUSED", "SICK_LEAVE", "MEDICAL_LEAVE", "OTHER"];

// DTO-shaped responses include sensitive fields that the overview must never display.
function record(id: number, overrides: Record<string, unknown> = {}) {
  return {
    id, userId: alice.id, userName: "Alice Student", userType: "STUDENT", date: "2024-02-29", status: "PRESENT",
    className: "Class 7A", courseName: `Course ${id}`, teacherName: "Theo Teacher", timetableSlotId: 5,
    remarks: "Private remark", excuse: "Private excuse", medicalNote: "Private medical note", ...overrides,
  };
}

function serve(studentId: number, respond: (params: URLSearchParams) => Response | Promise<Response>) {
  const requests: URLSearchParams[] = [];
  server.use(http.get(apiUrl(`/v1/attendance/user/${studentId}`), ({ request }) => {
    const params = new URL(request.url).searchParams;
    requests.push(params);
    return respond(params);
  }));
  return requests;
}
const success = (entries: ReturnType<typeof record>[]) => HttpResponse.json({ status: "success", data: entries });
function CurrentLocation() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname + location.search}</div>;
}
function renderPage(path = "/student/attendance?month=2024-02", lng: "en" | "fr" | "ar" | "zh-TW" = "en") {
  return render(
    <I18nextProvider i18n={createTestI18n(lng)}><Provider store={store}><QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}><StudentAttendance /><CurrentLocation /></MemoryRouter>
    </QueryClientProvider></Provider></I18nextProvider>,
  );
}
function logIn(user = alice) {
  store.dispatch(loginSuccess({ user, accessToken: `access-${user.id}`, refreshToken: `refresh-${user.id}` }));
}

let attendanceSignal: GenericAbortSignal | undefined;
let requestObserver: number;
beforeEach(() => {
  attendanceSignal = undefined;
  requestObserver = api.interceptors.request.use((config) => {
    if (config.url?.startsWith("/v1/attendance/user/")) attendanceSignal = config.signal;
    return config;
  });
  setSessionEndedHandler(() => store.dispatch(resetAuth()));
  logIn();
});
afterEach(() => {
  api.interceptors.request.eject(requestObserver);
  cleanup();
  terminateSession();
});

describe("student attendance history", () => {
  it("renders all seven recorded statuses, preserves same-day records and sorts dates then IDs", async () => {
    const entries = statuses.map((status, index) => record(index + 1, { status }));
    entries.push(record(8, { date: "2024-02-01" }));
    const requests = serve(alice.id, () => success([...entries].reverse()));
    renderPage("/student/attendance?month=2024-02&userId=999");

    const list = await screen.findByRole("list", { name: "Recorded attendance" });
    const rows = within(list).getAllByRole("listitem");
    expect(rows).toHaveLength(8);
    for (const [index, label] of ["Present", "Absent", "Late", "Excused", "Sick leave", "Medical leave", "Other"].entries()) {
      expect(within(rows[index]).getByText(label, { exact: true })).toBeVisible();
      expect(within(rows[index]).getByText(`Course ${index + 1}`)).toBeVisible();
      expect(rows[index].querySelector("time")).toHaveAttribute("datetime", "2024-02-29");
      expect(rows[index].querySelector("time")).toHaveTextContent("2024-02-29");
      expect(within(rows[index]).getByText("Class 7A")).toBeVisible();
    }
    expect(rows[7].querySelector("time")).toHaveAttribute("datetime", "2024-02-01");
    expect(requests).toHaveLength(1);
    expect(Object.fromEntries(requests[0])).toEqual({ startDate: "2024-02-01", endDate: "2024-02-29" });
    expect(screen.queryByText(/Private|%|streak|award/i)).not.toBeInTheDocument();
  });

  it("handles null optional names without invented people or courses", async () => {
    serve(alice.id, () => success([record(1, { className: null, courseName: null, teacherName: null, timetableSlotId: null })]));
    renderPage();
    const row = within(await screen.findByRole("list", { name: "Recorded attendance" })).getByRole("listitem");
    expect(within(row).getByText("Present")).toBeVisible();
    expect(within(row).getByText("No class or course recorded")).toBeVisible();
    expect(row).not.toHaveTextContent(/undefined|null|Theo|Class 7A/);
  });

  it.each([
    ["2023-02", "2023-02-28"], ["2024-02", "2024-02-29"], ["2000-02", "2000-02-29"],
    ["2100-02", "2100-02-28"], ["2026-04", "2026-04-30"], ["2026-12", "2026-12-31"],
  ])("requests the inclusive calendar bounds for %s", async (month, endDate) => {
    const requests = serve(alice.id, () => success([]));
    renderPage(`/student/attendance?month=${month}`);
    await screen.findByText("No attendance entries were recorded for this month.");
    expect(Object.fromEntries(requests[0])).toEqual({ startDate: `${month}-01`, endDate });
  });

  it.each(["", "?month=2024-2", "?month=2024-13", "?month=2024-00", "?month=0000-01", "?month=2024-02-extra", "?month=2024-02&month=2024-03"])(
    "defaults an absent or invalid month (%s) to the current local calendar month", async (search) => {
      const now = new Date();
      const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
      const requests = serve(alice.id, () => success([]));
      renderPage(`/student/attendance${search}`);
      await screen.findByText("No attendance entries were recorded for this month.");
      expect(requests[0].get("startDate")).toBe(`${currentMonth}-01`);
    },
  );

  it("navigates December–January in both directions and bookmarks the selected month", async () => {
    const requests = serve(alice.id, () => success([]));
    renderPage("/student/attendance?month=2025-12");
    await screen.findByText("No attendance entries were recorded for this month.");
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Next month" }));
    await waitFor(() => expect(requests.at(-1)?.get("startDate")).toBe("2026-01-01"));
    expect(screen.getByTestId("location")).toHaveTextContent("month=2026-01");
    await user.click(screen.getByRole("button", { name: "Previous month" }));
    await user.click(screen.getByRole("button", { name: "Previous month" }));
    await waitFor(() => expect(requests.at(-1)?.get("endDate")).toBe("2025-11-30"));
    expect(screen.getByTestId("location")).toHaveTextContent("month=2025-11");
  });

  it("keeps a previous month's records hidden while the next month loads", async () => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => { release = resolve; });
    serve(alice.id, async (params) => {
      if (params.get("startDate") === "2024-03-01") { await gate; return success([]); }
      return success([record(1)]);
    });
    renderPage();
    await screen.findByText("Course 1");
    await userEvent.setup().click(screen.getByRole("button", { name: "Next month" }));
    expect(screen.getByRole("status")).toHaveTextContent("Loading attendance");
    expect(screen.queryByText("Course 1")).not.toBeInTheDocument();
    expect(screen.queryByText("No attendance entries were recorded for this month.")).not.toBeInTheDocument();
    release();
    await screen.findByText("No attendance entries were recorded for this month.");
  });

  it.each([403, 500, "network"])("shows %s failures and retries without disguising them as empty data", async (failure) => {
    let failed = true;
    const requests = serve(alice.id, () => failed
      ? failure === "network" ? HttpResponse.error() : HttpResponse.json({ detail: "Internal sensitive error" }, { status: Number(failure) })
      : success([record(1)]));
    renderPage();
    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to load attendance");
    expect(screen.queryByText(/No attendance entries|Internal sensitive/)).not.toBeInTheDocument();
    expect(requests).toHaveLength(1);
    failed = false;
    await userEvent.setup().click(screen.getByRole("button", { name: "Retry" }));
    await screen.findByText("Course 1");
    expect(requests).toHaveLength(2);
  });

  it("ends an unauthorized session when renewal is rejected and never shows empty attendance", async () => {
    serve(alice.id, () => HttpResponse.json({}, { status: 401 }));
    server.use(http.post(apiUrl("/auth/refresh"), () => HttpResponse.json({}, { status: 401 })));
    renderPage();
    await screen.findByText("Sign in to view your attendance.");
    expect(token.access).toBeNull();
    expect(screen.queryByText("No attendance entries were recorded for this month.")).not.toBeInTheDocument();
    expect(screen.queryByRole("listitem")).not.toBeInTheDocument();
  });

  it("rejects the complete response if any entry belongs to another account", async () => {
    serve(alice.id, () => success([record(1), record(2, { userId: bob.id })]));
    renderPage();
    await screen.findByRole("alert");
    expect(screen.queryByRole("listitem")).not.toBeInTheDocument();
    expect(screen.queryByText(/Course 1|Course 2/)).not.toBeInTheDocument();
  });

  it.each([
    { status: "success", data: null },
    { status: "success", data: [record(1, { status: "UNKNOWN" })] },
    { status: "success", data: [record(1, { date: "2024-03-01" })] },
  ])("rejects invalid successful responses without inventing an empty month", async (body) => {
    serve(alice.id, () => HttpResponse.json(body));
    renderPage();
    await screen.findByRole("alert");
    expect(screen.queryByRole("listitem")).not.toBeInTheDocument();
    expect(screen.queryByText("No attendance entries were recorded for this month.")).not.toBeInTheDocument();
  });

  it("isolates account switches and hides data immediately on logout", async () => {
    serve(alice.id, () => success([record(1)]));
    serve(bob.id, async () => { await delay(80); return success([record(2, { userId: bob.id })]); });
    renderPage();
    await screen.findByText("Course 1");
    act(() => logIn(bob));
    expect(screen.queryByText("Course 1")).not.toBeInTheDocument();
    await screen.findByText("Course 2");
    act(() => terminateSession());
    expect(screen.queryByText("Course 2")).not.toBeInTheDocument();
    expect(screen.getByText("Sign in to view your attendance.")).toBeVisible();
    expect(queryClient.getQueryCache().getAll().filter((query) => query.state.data !== undefined)).toHaveLength(0);
  });

  it("does not show a late previous-account response after switching accounts", async () => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => { release = resolve; });
    const requests = serve(alice.id, async () => { await gate; return success([record(1)]); });
    serve(bob.id, () => success([record(2, { userId: bob.id })]));
    renderPage();
    await waitFor(() => expect(requests).toHaveLength(1));
    const aliceSignal = attendanceSignal;
    act(() => logIn(bob));
    await screen.findByText("Course 2");
    expect(aliceSignal?.aborted).toBe(true);
    await act(async () => { release(); await gate; });
    expect(screen.queryByText("Course 1")).not.toBeInTheDocument();
  });

  it("cancels a pending request on logout and discards its late response", async () => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => { release = resolve; });
    const requests = serve(alice.id, async () => { await gate; return success([record(1)]); });
    renderPage();
    await waitFor(() => expect(requests).toHaveLength(1));
    const pendingSignal = attendanceSignal;
    act(() => terminateSession());
    expect(pendingSignal?.aborted).toBe(true);
    await act(async () => { release(); await gate; });
    expect(screen.queryByText("Course 1")).not.toBeInTheDocument();
    expect(screen.getByText("Sign in to view your attendance.")).toBeVisible();
  });

  it("does not make write requests while browsing or retrying attendance", async () => {
    const writes: string[] = [];
    const listener = ({ request }: { request: Request }) => { if (request.method !== "GET") writes.push(request.method); };
    server.events.on("request:start", listener);
    try {
      let failed = true;
      serve(alice.id, () => failed ? HttpResponse.json({}, { status: 500 }) : success([]));
      renderPage();
      await screen.findByRole("alert");
      failed = false;
      await userEvent.setup().click(screen.getByRole("button", { name: "Retry" }));
      await screen.findByText("No attendance entries were recorded for this month.");
      await userEvent.setup().click(screen.getByRole("button", { name: "Next month" }));
      await screen.findByText("No attendance entries were recorded for this month.");
      expect(writes).toEqual([]);
    } finally { server.events.removeListener("request:start", listener); }
  });

  it.each([
    ["en", ["Present", "Absent", "Late", "Excused", "Sick leave", "Medical leave", "Other"]],
    ["fr", ["Présent", "Absent", "En retard", "Excusé", "Congé maladie", "Congé médical", "Autre"]],
    ["ar", ["حاضر", "غائب", "متأخر", "غياب بعذر", "إجازة مرضية", "إجازة طبية", "أخرى"]],
    ["zh-TW", ["出席", "缺席", "遲到", "已准假", "病假", "醫療假", "其他"]],
  ] as const)("translates every supported status in %s", async (locale, labels) => {
    serve(alice.id, () => success(statuses.map((status, index) => record(index + 1, { status }))));
    renderPage(undefined, locale);
    for (const label of labels) {
      expect(await screen.findByText(label, { exact: true })).toBeVisible();
    }
  });
});
