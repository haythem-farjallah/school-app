import { beforeAll, describe, expect, it } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import StudentsCreate from "./StudentCreate";
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

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/admin/students/create"]}>
        <Routes>
          <Route path="/admin/students/create" element={<StudentsCreate />} />
          <Route path="/admin/students" element={<p>Students list</p>} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

async function fillAndSubmit() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("First Name"), "Sam");
  await user.type(screen.getByLabelText("Last Name"), "Student");
  await user.type(screen.getByLabelText("Email Address"), "sam@school.test");
  // The select's label is not bound to its trigger, so find the trigger next to the label.
  const gradeLevel = within(screen.getByText("Grade Level").parentElement!).getByRole("combobox");
  await user.click(gradeLevel);
  await user.click(await screen.findByRole("option", { name: "High School" }));
  await user.click(screen.getByRole("button", { name: "Create Student" }));
}

describe("StudentsCreate", () => {
  it("posts the student and returns to the students list", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/students"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: student }, { status: 201 });
      }),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText("Students list")).toBeInTheDocument();
    expect(await screen.findByText("Student created successfully!")).toBeInTheDocument();
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

  it("shows the backend error and stays on the form when creation fails", async () => {
    server.use(
      http.post(apiUrl("/v1/students"), () =>
        HttpResponse.json({ status: 409, detail: "Email already in use" }, { status: 409 }),
      ),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText("Email already in use")).toBeInTheDocument();
    expect(screen.queryByText("Students list")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create Student" })).toBeInTheDocument();
  });
});
