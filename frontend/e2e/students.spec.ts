import { expect, test, type Page } from "@playwright/test";

// Deterministic account created by the backend "fixtures" profile (DevFixtureLoader).
const admin = { email: "admin@fixtures.school.test", password: "Fixture-Pass-2024" };

async function signIn(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill(admin.email);
  await page.getByLabel("Password").fill(admin.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);
}

function studentsApi(page: Page, method: string, path: RegExp) {
  return page.waitForResponse(
    (response) => response.request().method() === method && path.test(new URL(response.url()).pathname),
  );
}

async function search(page: Page, query: string) {
  const searched = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname.endsWith("/api/v1/students/search") &&
      new URL(response.url()).searchParams.get("q") === query,
  );
  await page.getByRole("searchbox", { name: "Search students" }).fill(query);
  expect((await searched).status()).toBe(200);
}

test("admin creates, finds, edits and deletes a student, and every change survives a reload", async ({ page }) => {
  // Unique per run, so the assertions cannot pass on records left by an earlier run.
  const runId = Date.now().toString().slice(-6);
  const email = `e2e.student.${runId}@fixtures.school.test`;
  const lastName = `Tester${runId}`;
  const consoleErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });

  await signIn(page);
  await page.goto("/admin/students");
  await expect(page.getByRole("heading", { level: 1, name: "Students" })).toBeVisible();
  await expect(page.getByText(/Total Pages|Selected/)).toHaveCount(0);

  // Create on the full-page form.
  await page.getByRole("link", { name: "Add student" }).first().click();
  await expect(page).toHaveURL(/\/admin\/students\/create$/);
  await page.getByLabel("First name").fill("Nour");
  await page.getByLabel("Last name").fill(lastName);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Birthday").fill("2011-05-09");
  await page.getByLabel("Gender").click();
  await page.getByRole("option", { name: "Female" }).click();
  await page.getByLabel("Grade level").click();
  await page.getByRole("option", { name: "Middle school" }).click();
  const created = studentsApi(page, "POST", /\/api\/v1\/students$/);
  await page.getByRole("button", { name: "Create student" }).click();
  const createdResponse = await created;
  expect(createdResponse.status()).toBe(201);
  const id: number = (await createdResponse.json()).data.id;
  await expect(page).toHaveURL(/\/admin\/students$/);
  await expect(page.getByText("Student created", { exact: true })).toBeVisible();

  // The list reloads cleanly and the backend search finds the new student.
  await page.reload();
  await expect(page.getByRole("heading", { level: 1, name: "Students" })).toBeVisible();
  await search(page, email);
  const row = page.getByRole("row", { name: new RegExp(`Nour ${lastName}`) });
  await expect(row).toContainText(email);
  await expect(row).toContainText("Middle school");
  await expect(row).toContainText("Female");

  // The search is in the URL, so it survives a reload.
  await page.reload();
  await expect(page.getByRole("searchbox", { name: "Search students" })).toHaveValue(email);
  await expect(row).toBeVisible();

  // Details, read twice.
  await row.getByRole("link", { name: `Nour ${lastName}` }).click();
  await expect(page).toHaveURL(new RegExp(`/admin/students/view/${id}$`));
  const personal = page.getByRole("region", { name: "Personal information" });
  await expect(personal).toContainText("May 9, 2011");
  await expect(personal).toContainText("Female");
  await page.reload();
  await expect(personal).toContainText(email);

  // Edit the fields PATCH supports.
  await page.getByRole("button", { name: "Edit student" }).click();
  const sheet = page.getByRole("dialog", { name: "Edit student" });
  await expect(sheet.getByLabel("Email")).toHaveCount(0);
  await sheet.getByLabel("First name").fill("Nora");
  await sheet.getByLabel("Enrollment year").fill("2023");
  const patched = studentsApi(page, "PATCH", new RegExp(`/api/v1/students/${id}$`));
  await sheet.getByRole("button", { name: "Save changes" }).click();
  const patchedResponse = await patched;
  expect(patchedResponse.status()).toBe(200);
  expect(JSON.parse(patchedResponse.request().postData() ?? "{}")).toEqual({
    firstName: "Nora",
    lastName,
    gradeLevel: "MIDDLE",
    enrollmentYear: 2023,
  });
  await expect(page.getByText("Student updated", { exact: true })).toBeVisible();
  await expect(sheet).toBeHidden();
  await expect(page.getByRole("region", { name: `Nora ${lastName}` })).toBeVisible();

  await page.reload();
  await expect(page.getByRole("region", { name: `Nora ${lastName}` })).toBeVisible();
  await expect(page.getByRole("region", { name: "Academic information" })).toContainText("2023");

  // Back to the list, then delete after confirming.
  await page.getByRole("link", { name: "Back to students" }).click();
  await expect(page).toHaveURL(/\/admin\/students$/);
  await search(page, email);
  await page.getByRole("button", { name: `Actions for Nora ${lastName}` }).click();
  await page.getByRole("menuitem", { name: "Delete" }).click();
  const dialog = page.getByRole("dialog", { name: `Delete Nora ${lastName}?` });
  const deleted = studentsApi(page, "DELETE", new RegExp(`/api/v1/students/${id}$`));
  await dialog.getByRole("button", { name: "Delete" }).click();
  expect((await deleted).status()).toBe(200);
  await expect(page.getByText(`Nora ${lastName} was deleted`, { exact: true })).toBeVisible();
  await expect(page.getByText("No students found")).toBeVisible();

  await page.reload();
  await expect(page.getByText("No students found")).toBeVisible();

  expect(consoleErrors).toEqual([]);
});
