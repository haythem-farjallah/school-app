import { execFileSync, spawnSync } from "node:child_process";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { expect, test, type Page } from "@playwright/test";
import type { TimetableResponseDto } from "../src/types/timetable";

// These tests authenticate against Spring Boot and read persisted development slots.
// No /me response is fabricated. Traces are disabled to keep credentials out of artifacts.
test.use({ trace: "off" });

async function signIn(page: Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill(email);
  await page.getByLabel("Password").fill("Fixture-Pass-2024");
  const response = page.waitForResponse((r) => r.url().endsWith("/api/auth/login"));
  await page.getByRole("button", { name: "Sign In" }).click();
  const login = await response;
  // Respect the real shared login budget when the complete smoke suite runs.
  if (login.status() === 429) {
    const seconds = Number(login.headers()["retry-after"] ?? 60);
    await page.waitForTimeout(Math.min(Math.max(seconds, 1), 60) * 1000 + 1000);
    const retry = page.waitForResponse((r) => r.url().endsWith("/api/auth/login"));
    await page.getByRole("button", { name: "Sign In" }).click();
    expect((await retry).status()).toBe(200);
  } else {
    expect(login.status()).toBe(200);
  }
  await expect(page).toHaveURL(/\/$/);
}

const timetableResponse = (page: Page) => page.waitForResponse((r) =>
  new URL(r.url()).pathname === "/api/v1/timetables/me" && r.request().method() === "GET",
);

async function compareVisibleSlots(page: Page, slots: TimetableResponseDto["slots"]) {
  const displayed = page.locator('[data-testid="timetable-slot"]:visible');
  await expect(displayed).toHaveCount(slots.length);
  for (const slot of slots) {
    const item = page.locator(`[data-testid="timetable-slot"][data-slot-id="${slot.id}"]:visible`);
    await expect(item).toContainText(slot.forCourseName!);
    await expect(item).toContainText(slot.forClassName!);
    await expect(item).toContainText(`${slot.teacherFirstName} ${slot.teacherLastName}`);
    await expect(item).toContainText(slot.roomName!);
    await expect(item).toContainText(`${slot.periodStartTime!.slice(0, 5)} – ${slot.periodEndTime!.slice(0, 5)}`);
  }
}

test("real persisted timetable matches the browser on desktop and mobile, and survives loading and network errors", async ({ page }, testInfo) => {
  test.setTimeout(150_000);
  await signIn(page, "student@fixtures.school.test");
  await page.setViewportSize({ width: 1440, height: 1000 });

  let release!: () => void;
  const gate = new Promise<void>((resolve) => { release = resolve; });
  // Delay the genuine request to observe loading; continue sends it to Spring unchanged.
  await page.route("**/api/v1/timetables/me", async (route) => {
    await gate;
    await route.continue();
  });
  let responsePromise = timetableResponse(page);
  await page.getByRole("navigation", { name: "Main navigation" }).getByRole("link", { name: "My Timetable" }).click();
  await expect(page.getByRole("status")).toContainText("Loading your timetable");
  release();
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  const body = await response.json() as { data: TimetableResponseDto };
  await page.unroute("**/api/v1/timetables/me");

  const scratch = mkdtempSync(join(tmpdir(), "school-timetable-"));
  try {
    const capture = join(scratch, "api.json");
    writeFileSync(capture, JSON.stringify(body));
    const output = execFileSync("python3", ["../scripts/verify-timetable-demo.py", "--response", capture], { encoding: "utf8" });
    expect(output).toContain("PASS: /api/v1/timetables/me equals persisted");
    await testInfo.attach("api-to-postgresql-verification", { body: output, contentType: "text/plain" });
    const altered = structuredClone(body);
    altered.data.slots[0].periodStartTime = "03:00:00";
    writeFileSync(capture, JSON.stringify(altered));
    const rejected = spawnSync("python3", ["../scripts/verify-timetable-demo.py", "--response", capture], { encoding: "utf8" });
    expect(rejected.status).toBe(1);
    expect(rejected.stderr).toContain("API differs from PostgreSQL");
  } finally {
    rmSync(scratch, { recursive: true, force: true });
  }
  await testInfo.attach("real-timetable-response", { body: JSON.stringify(body, null, 2), contentType: "application/json" });
  const table = page.getByRole("table", { name: "Weekly timetable" });
  await expect(table).toBeVisible();
  for (const day of ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]) {
    await expect(table.getByRole("columnheader", { name: day, exact: true })).toBeVisible();
  }
  await compareVisibleSlots(page, body.data.slots);
  for (const slot of body.data.slots) {
    const dayIndex = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"].indexOf(slot.dayOfWeek);
    const item = page.locator(`[data-slot-id="${slot.id}"]`);
    const row = table.getByRole("row").filter({ has: item });
    await expect(row.getByRole("cell").nth(dayIndex).locator(`[data-slot-id="${slot.id}"]`)).toBeVisible();
  }
  await page.screenshot({ path: testInfo.outputPath("timetable-desktop.png"), fullPage: true });
  await testInfo.attach("desktop", { path: testInfo.outputPath("timetable-desktop.png"), contentType: "image/png" });

  await page.setViewportSize({ width: 360, height: 800 });
  await expect(table).toBeHidden();
  const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
  for (const day of days) {
    await page.getByRole("combobox", { name: "Day", exact: true }).selectOption(day);
    await compareVisibleSlots(page, body.data.slots.filter((slot) => slot.dayOfWeek === day));
    expect(await page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth")).toBe(true);
  }
  await page.getByRole("combobox", { name: "Day", exact: true }).selectOption("MONDAY");
  await page.screenshot({ path: testInfo.outputPath("timetable-mobile.png"), fullPage: true });
  await testInfo.attach("mobile", { path: testInfo.outputPath("timetable-mobile.png"), contentType: "image/png" });

  // Fail a later request at the network boundary; no synthetic response JSON is supplied.
  await page.getByRole("button", { name: "Open navigation menu" }).click();
  await page.getByRole("menuitem", { name: "Home", exact: true }).click();
  await page.route("**/api/v1/timetables/me", (route) => route.abort("connectionrefused"));
  await page.getByRole("button", { name: "Open navigation menu" }).click();
  await page.getByRole("menuitem", { name: "My Timetable" }).click();
  await expect(page.getByRole("alert")).toContainText("Your timetable could not be loaded");
  await expect(page.locator('[data-testid="timetable-slot"]:visible')).toHaveCount(0);
  await page.unroute("**/api/v1/timetables/me");
  responsePromise = timetableResponse(page);
  await page.getByRole("button", { name: "Try again" }).click();
  expect((await responsePromise).status()).toBe(200);
  await compareVisibleSlots(page, body.data.slots.filter((slot) => slot.dayOfWeek === "MONDAY"));

  responsePromise = timetableResponse(page);
  await page.goto("/student/schedule");
  await expect(page).toHaveURL(/\/student\/my-timetable$/);
  expect((await responsePromise).status()).toBe(200);
  await expect(page.getByRole("button", { name: /edit|save|delete/i })).toHaveCount(0);
});

test("a development student with no enrollment gets a real successful empty timetable", async ({ page }, testInfo) => {
  test.setTimeout(150_000);
  await signIn(page, "empty.student@fixtures.school.test");
  const response = timetableResponse(page);
  await page.goto("/student/my-timetable");
  const empty = await response;
  expect(empty.status()).toBe(200);
  expect((await empty.json() as { data: TimetableResponseDto }).data.slots).toEqual([]);
  await expect(page.getByText("No timetable slots to display")).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect(page.locator('[data-testid="timetable-slot"]:visible')).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath("timetable-empty.png"), fullPage: true });
});
