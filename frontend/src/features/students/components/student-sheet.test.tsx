import { beforeAll, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "react-hot-toast";
import { AddStudentSheet } from "./student-sheet";
import { apiUrl, server } from "@/test/server";
import type { Student } from "@/types/student";

// jsdom lacks the pointer capture and scroll APIs the Radix Select calls, and the
// matchMedia the toaster calls.
beforeAll(() => {
  window.matchMedia = (query: string) =>
    ({ matches: false, media: query, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList;
  Element.prototype.hasPointerCapture = () => false;
  Element.prototype.releasePointerCapture = () => {};
  Element.prototype.scrollIntoView = () => {};
});

const student: Student = {
  id: 5,
  firstName: "Sam",
  lastName: "Student",
  email: "sam@school.test",
  gradeLevel: "HIGH",
  enrollmentYear: new Date().getFullYear(),
};

function renderSheet(onSuccess: () => void) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <AddStudentSheet onSuccess={onSuccess} />
      <Toaster />
    </QueryClientProvider>,
  );
}

async function openFillAndSubmit() {
  const user = userEvent.setup();
  await user.click(screen.getByRole("button", { name: "Add Student" }));
  const sheet = await screen.findByRole("dialog");
  await user.type(within(sheet).getByLabelText("First Name"), "Sam");
  await user.type(within(sheet).getByLabelText("Last Name"), "Student");
  await user.type(within(sheet).getByLabelText("Email Address"), "sam@school.test");
  // The select's label is not bound to its trigger, so find the trigger next to the label.
  await user.click(within(within(sheet).getByText("Grade Level").parentElement!).getByRole("combobox"));
  await user.click(await screen.findByRole("option", { name: "High School" }));
  await user.click(within(sheet).getByRole("button", { name: "Add Student" }));
}

describe("AddStudentSheet", () => {
  it("posts the student, closes the sheet and reports success", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/students"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: student }, { status: 201 });
      }),
    );
    const onSuccess = vi.fn();
    renderSheet(onSuccess);

    await openFillAndSubmit();

    expect(await screen.findByText("Student added successfully!")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(body).toEqual({
      profile: {
        firstName: "Sam",
        lastName: "Student",
        email: "sam@school.test",
        telephone: "",
        birthday: "",
        address: "",
      },
      gradeLevel: "HIGH",
      enrollmentYear: new Date().getFullYear(),
    });
  });

  it("shows the backend error and keeps the sheet open when creation fails", async () => {
    server.use(
      http.post(apiUrl("/v1/students"), () =>
        HttpResponse.json({ status: 409, detail: "Email already in use" }, { status: 409 }),
      ),
    );
    const onSuccess = vi.fn();
    renderSheet(onSuccess);

    await openFillAndSubmit();

    expect(await screen.findByText("Email already in use")).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });
});
