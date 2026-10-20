import { expect, test } from "@playwright/test";
import type { AttendanceHistoryRecord } from "../src/types/attendance";
import { AttendanceStatus } from "../src/types/attendance";

// This test is isolated from backend data. The real-stack comparison lives in student-attendance.spec.ts.
test("isolated populated, loading and failure views fit desktop and mobile", async ({ page }) => {
  const user = { id: 21, firstName: "Alice", lastName: "Student", email: "alice@fixtures.school.test", role: "STUDENT" };
  await page.addInitScript(`
    localStorage.setItem("accessToken", "isolated-attendance-test");
    localStorage.setItem("refreshToken", "isolated-attendance-refresh");
    localStorage.setItem("userData", ${JSON.stringify(JSON.stringify(user))});
    localStorage.setItem("lang", "en");
    localStorage.setItem("i18nextLng", "en");
  `);
  const statuses: AttendanceHistoryRecord["status"][] = [
    AttendanceStatus.PRESENT, AttendanceStatus.ABSENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED,
    "SICK_LEAVE", "MEDICAL_LEAVE", "OTHER",
  ];
  const records: AttendanceHistoryRecord[] = statuses.map((status, index) => ({
    id: index + 1, userId: user.id, status, date: "2024-02-29", timetableSlotId: null, teacherName: null,
    className: index === 0 ? "A".repeat(160) : null, courseName: index === 0 ? "Course ".repeat(30) : null,
  }));
  const writes: string[] = [];
  let fail = false;
  let release!: () => void;
  let gate = Promise.resolve();
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    if (request.method() !== "GET") writes.push(request.method());
    const url = new URL(request.url());
    expect(url.pathname).toBe(`/api/v1/attendance/user/${user.id}`);
    expect(Object.fromEntries(url.searchParams)).toEqual({ startDate: "2024-02-01", endDate: "2024-02-29" });
    await gate;
    await route.fulfill({ status: fail ? 503 : 200, json: fail ? { detail: "Internal error" } : { status: "success", data: records } });
  });
  // Block websocket setup too: this isolated rendering test must not contact the backend.
  await page.route("**/ws/**", (route) => route.abort());

  for (const width of [1440, 390]) {
    await page.setViewportSize({ width, height: 900 });
    gate = new Promise<void>((resolve) => { release = resolve; });
    const request = page.waitForRequest((request) => request.url().includes(`/api/v1/attendance/user/${user.id}`));
    await page.goto("/student/attendance?month=2024-02");
    await request;
    const main = page.getByRole("main");
    await expect(main.getByRole("status")).toHaveText("Loading attendance");
    await expect(main.getByRole("listitem")).toHaveCount(0);
    release();
    const rows = main.getByRole("list", { name: "Recorded attendance" }).getByRole("listitem");
    await expect(rows).toHaveCount(7);
    for (const label of ["Present", "Absent", "Late", "Excused", "Sick leave", "Medical leave", "Other"]) {
      await expect(main.getByText(label, { exact: true })).toBeVisible();
    }
    await expect(main.getByText("No class or course recorded")).toHaveCount(6);
    expect(await page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth")).toBe(true);
    fail = true;
    await page.reload();
    await expect(main.getByRole("alert")).toContainText("Unable to load attendance");
    await expect(main.getByRole("listitem")).toHaveCount(0);
    fail = false;
    await main.getByRole("button", { name: "Retry" }).click();
    await expect(rows).toHaveCount(7);
  }
  expect(writes).toEqual([]);
});
