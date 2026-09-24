import { expect, test, type Page } from "@playwright/test";

// Deterministic account created by the backend "fixtures" profile (DevFixtureLoader).
const admin = { email: "admin@fixtures.school.test", password: "Fixture-Pass-2024" };

function currentUserResponse(page: Page) {
  return page.waitForResponse(
    (response) => response.url().endsWith("/api/v1/dashboard/current-user") && response.request().method() === "GET",
  );
}

test("admin logs in, reaches the admin dashboard and stays signed in after reload", async ({ page }) => {
  await page.goto("/admin/dashboard");
  await expect(page).toHaveURL(/\/login$/);

  await page.getByLabel("Email Address").fill(admin.email);
  await page.getByLabel("Password").fill(admin.password);
  const loginResponse = page.waitForResponse((response) => response.url().endsWith("/api/auth/login"));
  await page.getByRole("button", { name: "Sign In" }).click();
  expect((await loginResponse).status()).toBe(200);
  await expect(page).toHaveURL(/\/$/);

  let currentUser = currentUserResponse(page);
  await page.goto("/admin/dashboard");
  await expect(page.getByRole("heading", { name: "Admin Dashboard" })).toBeVisible();
  let response = await currentUser;
  expect(response.status()).toBe(200);
  expect(response.request().headers()["authorization"]).toMatch(/^Bearer /);

  currentUser = currentUserResponse(page);
  await page.reload();
  await expect(page).toHaveURL(/\/admin\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Admin Dashboard" })).toBeVisible();
  response = await currentUser;
  expect(response.status()).toBe(200);
  expect(response.request().headers()["authorization"]).toMatch(/^Bearer /);
});
