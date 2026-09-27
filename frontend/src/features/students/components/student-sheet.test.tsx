import { beforeAll, describe, expect, it } from "vitest";
import { useState } from "react";
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { EditStudentSheet } from "./student-sheet";
import { studentKeys } from "../hooks/use-students";
import { apiUrl, server } from "@/test/server";
import type { SupportedLocale } from "@/services/i18n";
import { installDomShims, renderStudentsRoute, student } from "../test-utils";

beforeAll(installDomShims);

function EditHarness() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button onClick={() => setOpen(true)}>Open editor</button>
      <EditStudentSheet student={student} open={open} onOpenChange={setOpen} />
    </>
  );
}

async function openSheet(lng: SupportedLocale = "en") {
  const rendered = renderStudentsRoute(<EditHarness />, { url: "/admin/students", path: "/admin/students", lng });
  const user = userEvent.setup();
  await user.click(screen.getByRole("button", { name: "Open editor" }));
  return { ...rendered, user, sheet: await screen.findByRole("dialog") };
}

describe("EditStudentSheet", () => {
  it("edits only the four fields the backend update accepts", async () => {
    const { sheet } = await openSheet();

    expect(within(sheet).getByRole("heading", { name: "Edit student" })).toBeInTheDocument();
    expect(within(sheet).getByLabelText("First name")).toHaveValue("Sam");
    expect(within(sheet).getByLabelText("Last name")).toHaveValue("Student");
    expect(within(sheet).getByLabelText("Grade level")).toHaveTextContent("High school");
    expect(within(sheet).getByLabelText("Enrollment year")).toHaveValue(2024);
    for (const label of ["Email", "Phone", "Birthday", "Gender", "Address"]) {
      expect(within(sheet).queryByLabelText(label)).not.toBeInTheDocument();
    }
    expect(sheet).toHaveTextContent("Email, phone, birthday, gender and address cannot be changed here.");
  });

  it("patches exactly the edited fields, closes and makes the student query fresh", async () => {
    let body: unknown;
    const updated = { ...student, firstName: "Samira", enrollmentYear: 2025 };
    server.use(
      http.patch(apiUrl("/v1/students/5"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: updated });
      }),
    );
    const { sheet, user, queryClient } = await openSheet();
    queryClient.setQueryData(studentKeys.list({ page: 0, size: 10, search: "" }), { data: [student] });

    await user.clear(within(sheet).getByLabelText("First name"));
    await user.type(within(sheet).getByLabelText("First name"), "Samira");
    await user.clear(within(sheet).getByLabelText("Enrollment year"));
    await user.type(within(sheet).getByLabelText("Enrollment year"), "2025");
    await user.click(within(sheet).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByText("Student updated")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(body).toEqual({ firstName: "Samira", lastName: "Student", gradeLevel: "HIGH", enrollmentYear: 2025 });
    expect(queryClient.getQueryData(studentKeys.detail(5))).toEqual(updated);
    expect(queryClient.getQueryState(studentKeys.list({ page: 0, size: 10, search: "" }))?.isInvalidated).toBe(true);
  });

  it("stays open and usable when the update fails", async () => {
    let attempts = 0;
    server.use(
      http.patch(apiUrl("/v1/students/5"), () => {
        attempts += 1;
        return HttpResponse.json({ status: 400, detail: "Grade level is not valid" }, { status: 400 });
      }),
    );
    const { sheet, user } = await openSheet();

    await user.click(within(sheet).getByRole("button", { name: "Save changes" }));

    expect(await screen.findByText("Grade level is not valid")).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(within(sheet).getByLabelText("First name")).toHaveValue("Sam");
    await user.click(within(sheet).getByRole("button", { name: "Save changes" }));
    await waitFor(() => expect(attempts).toBe(2));
  });

  it("validates with translated messages", async () => {
    const { sheet, user } = await openSheet();

    await user.clear(within(sheet).getByLabelText("First name"));
    await user.clear(within(sheet).getByLabelText("Enrollment year"));
    await user.type(within(sheet).getByLabelText("Enrollment year"), "1800");
    await user.click(within(sheet).getByRole("button", { name: "Save changes" }));

    expect(await within(sheet).findByText("Enter a first name.")).toBeInTheDocument();
    expect(within(sheet).getByText("Enter a year between 1900 and 2100.")).toBeInTheDocument();
  });

  it("opens from the inline end in Arabic, fully translated", async () => {
    const { sheet } = await openSheet("ar");

    expect(within(sheet).getByRole("heading", { name: "تعديل الطالب" })).toBeInTheDocument();
    expect(within(sheet).getByLabelText("الاسم الأول")).toHaveValue("Sam");
    const gradeLevel = within(sheet).getByLabelText("المرحلة الدراسية");
    expect(gradeLevel).toHaveTextContent("المرحلة الثانوية");
    expect(gradeLevel).toHaveAttribute("dir", "rtl");
    expect(within(sheet).getByRole("button", { name: "حفظ التغييرات" })).toBeInTheDocument();
    // In right-to-left the inline end is the physical left.
    expect(sheet.className).toMatch(/\bleft-0\b/);
    expect(sheet.textContent).not.toMatch(/\bstudents\.[a-zA-Z]/);
  });
});
