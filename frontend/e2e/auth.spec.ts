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

test("a session the backend no longer accepts ends on the login page", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill(admin.email);
  await page.getByLabel("Password").fill(admin.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);

  // Simulate an expired/revoked token: the app still holds the user, the backend rejects the token.
  await page.evaluate(() => localStorage.setItem("accessToken", "revoked-token"));
  await page.reload();
  const profileResponse = page.waitForResponse(
    (response) => response.url().endsWith("/api/me/profile") && response.request().method() === "GET",
  );
  await page.goto("/admin/profile");

  expect((await profileResponse).status()).toBe(401);
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("button", { name: "Sign In" })).toBeVisible();
  expect(await page.evaluate(() => [localStorage.getItem("accessToken"), localStorage.getItem("refreshToken")])).toEqual([
    null,
    null,
  ]);
});

test("admin signs out and protected pages require a new login", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill(admin.email);
  await page.getByLabel("Password").fill(admin.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);
  await page.goto("/admin/dashboard");
  await expect(page.getByRole("heading", { name: "Admin Dashboard" })).toBeVisible();

  await page.getByRole("button", { name: /Ada Admin/ }).click();
  await page.getByRole("menuitem", { name: /Sign out/ }).click();

  await expect(page).toHaveURL(/\/login$/);
  expect(await page.evaluate(() => [localStorage.getItem("accessToken"), localStorage.getItem("refreshToken")])).toEqual([
    null,
    null,
  ]);
  await page.goto("/admin/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});

test("student logs out from the sidebar and protected pages require a new login", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill("student@fixtures.school.test");
  await page.getByLabel("Password").fill("Fixture-Pass-2024");
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);
  await page.goto("/student/exams");

  await page.getByRole("button", { name: "Logout", exact: true }).click();

  await expect(page).toHaveURL(/\/login$/);
  expect(await page.evaluate(() => [localStorage.getItem("accessToken"), localStorage.getItem("refreshToken")])).toEqual([
    null,
    null,
  ]);
  await page.goto("/student/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});
