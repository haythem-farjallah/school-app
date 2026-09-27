import { expect, test, type Page } from "@playwright/test";

// Deterministic account created by the backend "fixtures" profile (DevFixtureLoader).
const admin = { email: "admin@fixtures.school.test", password: "Fixture-Pass-2024" };

// The admin dashboard reads its statistics from GET /api/v1/dashboard/admin/{adminId}.
function adminDashboardResponse(page: Page) {
  return page.waitForResponse(
    (response) => /\/api\/v1\/dashboard\/admin\/\d+$/.test(response.url()) && response.request().method() === "GET",
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

  let dashboard = adminDashboardResponse(page);
  await page.goto("/admin/dashboard");
  await expect(page.getByRole("heading", { name: "Admin Dashboard" })).toBeVisible();
  let response = await dashboard;
  expect(response.status()).toBe(200);
  expect(response.request().headers()["authorization"]).toMatch(/^Bearer /);

  dashboard = adminDashboardResponse(page);
  await page.reload();
  await expect(page).toHaveURL(/\/admin\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Admin Dashboard" })).toBeVisible();
  response = await dashboard;
  expect(response.status()).toBe(200);
  expect(response.request().headers()["authorization"]).toMatch(/^Bearer /);
});

async function logInAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill(admin.email);
  await page.getByLabel("Password").fill(admin.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);
}

function storedTokens(page: Page) {
  return page.evaluate(() => ({
    accessToken: localStorage.getItem("accessToken"),
    refreshToken: localStorage.getItem("refreshToken"),
  }));
}

function refreshResponse(page: Page) {
  return page.waitForResponse(
    (response) => response.url().endsWith("/api/auth/refresh") && response.request().method() === "POST",
  );
}

test("a rejected access token is renewed with the refresh token and the page keeps working", async ({ page }) => {
  await logInAsAdmin(page);
  const { refreshToken } = await storedTokens(page);
  expect(refreshToken).toBeTruthy();

  // Simulate an expired/revoked access token while the refresh token is still valid.
  const rejected = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/me/profile") && response.request().headers()["authorization"] === "Bearer revoked-token",
  );
  const refreshed = refreshResponse(page);
  const retried = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/me/profile") &&
      response.request().method() === "GET" &&
      /^Bearer (?!revoked-token$)/.test(response.request().headers()["authorization"] ?? ""),
  );
  await page.evaluate(() => localStorage.setItem("accessToken", "revoked-token"));
  await page.goto("/admin/profile");

  expect((await rejected).status()).toBe(401);
  const refresh = await refreshed;
  expect(refresh.status()).toBe(200);
  expect(refresh.request().postDataJSON()).toEqual({ refreshToken });
  expect((await retried).status()).toBe(200);

  await expect(page).toHaveURL(/\/admin\/profile$/);
  const tokens = await storedTokens(page);
  expect(tokens.accessToken).toBeTruthy();
  expect(tokens.accessToken).not.toBe("revoked-token");
  expect(tokens.refreshToken).toBe(refreshToken);
  await expect(page.getByRole("button", { name: "Sign In" })).toHaveCount(0);
});

test("a session whose refresh token is also rejected ends on the login page", async ({ page }) => {
  await logInAsAdmin(page);

  // Neither token is accepted any more: the refresh fails and the session ends.
  const rejected = page.waitForResponse(
    (response) =>
      response.url().includes("/api/") && response.request().headers()["authorization"] === "Bearer revoked-token",
  );
  const refreshed = refreshResponse(page);
  await page.evaluate(() => {
    localStorage.setItem("accessToken", "revoked-token");
    localStorage.setItem("refreshToken", "invalid-refresh-token");
  });
  await page.goto("/admin/profile");

  const response = await rejected;
  expect(response.status()).toBe(401);
  expect(response.headers()["content-type"]).toContain("application/problem+json");
  const refresh = await refreshed;
  expect(refresh.status()).toBe(401);
  expect(refresh.headers()["content-type"]).toContain("application/problem+json");
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("button", { name: "Sign In" })).toBeVisible();
  expect(await storedTokens(page)).toEqual({ accessToken: null, refreshToken: null });
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
