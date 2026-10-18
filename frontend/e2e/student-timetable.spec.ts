import { expect, test } from "@playwright/test";
import { studentTimetable } from "../src/test/fixtures/student-timetable";

// Real authentication and routing; the dev accounts have no academic-year enrollment.
// Supply the backend DTO fixture for the rendering checks. Backend integration tests
// cover /me's enrollment resolution and security against PostgreSQL.
test("student opens a read-only timetable on desktop, tablet and phone, including weekend classes and the old URL", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill("student@fixtures.school.test");
  await page.getByLabel("Password").fill("Fixture-Pass-2024");
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);

  const requests: string[] = [];
  await page.route("**/api/v1/timetables/me", async (route) => {
    expect(route.request().method()).toBe("GET");
    expect(route.request().headers()["authorization"]).toMatch(/^Bearer \S+$/);
    await route.fulfill({ json: studentTimetable });
  });
  page.on("request", (request) => {
    const path = new URL(request.url()).pathname;
    if (/\/api\/v1\/(timetables|dashboard|periods|classes)/.test(path)) requests.push(path);
  });

  await page.getByRole("navigation", { name: "Main navigation" }).getByRole("link", { name: "My Timetable" }).click();
  await expect(page).toHaveURL(/\/student\/my-timetable$/);
  await expect(page.getByRole("heading", { level: 1, name: "My Timetable" })).toBeVisible();
  await expect(page.getByText("Mathematics", { exact: true })).toBeVisible();
  await expect(page.getByRole("region", { name: "Saturday" })).toContainText("Art");
  await expect(page.getByRole("region", { name: "Sunday" })).toContainText("History");
  await expect(page.getByRole("button", { name: /edit|save|delete/i })).toHaveCount(0);

  await page.goto("/student/schedule");
  await expect(page).toHaveURL(/\/student\/my-timetable$/);
  await expect(page.getByText("Mathematics", { exact: true })).toBeVisible();

  for (const width of [800, 360]) {
    await page.setViewportSize({ width, height: 740 });
    await page.getByRole("button", { name: "Open navigation menu" }).click();
    await page.getByRole("menuitem", { name: "Home", exact: true }).click();
    await expect(page).toHaveURL(/\/$/);
    await page.getByRole("button", { name: "Open navigation menu" }).click();
    await page.getByRole("menuitem", { name: "My Timetable" }).click();
    await expect(page).toHaveURL(/\/student\/my-timetable$/);
    await expect(page.getByRole("menu")).toHaveCount(0);
    await expect(page.getByText("Mathematics", { exact: true })).toBeVisible();
    await expect(page.getByRole("region", { name: "Sunday" })).toContainText("History");
    expect(await page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth")).toBe(true);
  }
  expect(requests.length).toBeGreaterThan(0);
  expect(requests.every((path) => path === "/api/v1/timetables/me")).toBe(true);
});
