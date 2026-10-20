import { expect, test } from "@playwright/test";
import type { AttendanceHistoryRecord } from "../src/types/attendance";

const statusLabels: Record<string, string> = {
  PRESENT: "Present", ABSENT: "Absent", LATE: "Late", EXCUSED: "Excused",
  SICK_LEAVE: "Sick leave", MEDICAL_LEAVE: "Medical leave", OTHER: "Other",
};

test("student attendance agrees with the real authenticated response on desktop and mobile", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill("student@fixtures.school.test");
  await page.getByLabel("Password").fill("Fixture-Pass-2024");
  const loginResponse = page.waitForResponse((response) => response.url().endsWith("/api/auth/login"));
  await page.getByRole("button", { name: "Sign In" }).click();
  const login = await loginResponse;
  expect(login.status()).toBe(200);
  const { data: { user: { id: userId } } }: { data: { user: { id: number } } } = await login.json();
  await expect(page).toHaveURL(/\/$/);

  const writes: string[] = [];
  const reads: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("/api/") && request.method() !== "GET") writes.push(request.method());
    if (request.url().includes("/api/v1/attendance")) reads.push(new URL(request.url()).pathname);
  });

  for (const width of [1440, 390]) {
    await test.step(`Compare actual records at ${width}px`, async () => {
      await page.setViewportSize({ width, height: 900 });
      // Capture the browser's real request; no interception, substituted data or database writes.
      const pending = page.waitForResponse((response) => /\/api\/v1\/attendance\/user\/\d+\?/.test(response.url()));
      await page.goto("/student/attendance");
      const response = await pending;
      expect(response.status()).toBe(200);
      expect(response.request().method()).toBe("GET");
      expect(Boolean(response.request().headers()["authorization"]?.startsWith("Bearer "))).toBe(true);
      const url = new URL(response.url());
      expect(url.pathname).toBe(`/api/v1/attendance/user/${userId}`);
      // Use the browser's local calendar, independently of the app's month helper.
      const month = await page.evaluate<{ start: string; end: string }>(`(() => {
        const now = new Date();
        const start = [now.getFullYear(), String(now.getMonth() + 1).padStart(2, "0"), "01"].join("-");
        const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
        return { start, end: start.slice(0, 8) + lastDay };
      })()`);
      expect(Object.fromEntries(url.searchParams)).toEqual({ startDate: month.start, endDate: month.end });
      const { data }: { data: AttendanceHistoryRecord[] } = await response.json();
      expect(Array.isArray(data)).toBe(true);
      expect(data.every((record) => record.userId === userId)).toBe(true);
      test.info().annotations.push({ type: "attendance-records", description: `${width}px: ${data.length} recorded entries for ${month.start.slice(0, 7)}` });
      const records = [...data].sort((a, b) => b.date.localeCompare(a.date) || a.id - b.id);
      const main = page.getByRole("main");
      await expect(main.getByRole("heading", { level: 1, name: "My Attendance" })).toBeVisible();
      if (records.length === 0) {
        await expect(main.getByText("No attendance entries were recorded for this month.")).toBeVisible();
        await expect(main.getByRole("listitem")).toHaveCount(0);
      } else {
        const rows = main.getByRole("list", { name: "Recorded attendance" }).getByRole("listitem");
        await expect(rows).toHaveCount(records.length);
        for (const [index, record] of records.entries()) {
          const row = rows.nth(index);
          await expect(row.locator("time")).toHaveAttribute("datetime", record.date);
          await expect(row.locator("time")).toHaveText(record.date);
          await expect(row.getByText(statusLabels[record.status], { exact: true })).toBeVisible();
          if (record.className) await expect(row.getByText(record.className, { exact: true })).toBeVisible();
          if (record.courseName) await expect(row.getByText(record.courseName, { exact: true })).toBeVisible();
          if (!record.className && !record.courseName) await expect(row.getByText("No class or course recorded")).toBeVisible();
        }
      }
      await expect(main.getByRole("alert")).toHaveCount(0);
      await expect(main.getByText(/%|streak|award/i)).toHaveCount(0);
      expect(await page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth")).toBe(true);
      expect(await page.evaluate("document.body.scrollWidth <= document.documentElement.clientWidth")).toBe(true);
    });
  }
  expect(reads).toEqual([`/api/v1/attendance/user/${userId}`, `/api/v1/attendance/user/${userId}`]);
  expect(writes).toEqual([]);
});
