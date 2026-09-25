import { expect, test } from "@playwright/test";

// Deterministic account created by the backend "fixtures" profile (DevFixtureLoader).
const teacher = { email: "teacher@fixtures.school.test", password: "Fixture-Pass-2024" };

test("teacher updates contact details and they persist after reload", async ({ page }) => {
  // Unique per run, so the assertions cannot pass on values left by an earlier run.
  const runId = Date.now().toString().slice(-6);
  const telephone = `+216 20 ${runId}`;
  const address = `${runId} Avenue Habib Bourguiba, Tunis`;

  await page.goto("/login");
  await page.getByLabel("Email Address").fill(teacher.email);
  await page.getByLabel("Password").fill(teacher.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);

  await page.goto("/teacher/profile");
  await page.getByLabel("Phone Number").fill(telephone);
  await page.getByLabel("Address").fill(address);
  const patchResponse = page.waitForResponse(
    (response) => response.url().endsWith("/api/me/profile") && response.request().method() === "PATCH",
  );
  await page.getByRole("button", { name: "Update Contact Info" }).click();
  const patched = await patchResponse;
  expect(patched.status()).toBe(200);
  expect(await patched.json()).toMatchObject({ email: teacher.email, telephone, address });

  const profileResponse = page.waitForResponse(
    (response) => response.url().endsWith("/api/me/profile") && response.request().method() === "GET",
  );
  await page.reload();
  expect(await (await profileResponse).json()).toMatchObject({ telephone, address });
  await expect(page.getByText(telephone, { exact: true })).toBeVisible();
  await expect(page.getByText(address, { exact: true })).toBeVisible();
  await expect(page.getByLabel("Phone Number")).toHaveValue(telephone);
  await expect(page.getByLabel("Address")).toHaveValue(address);
});
