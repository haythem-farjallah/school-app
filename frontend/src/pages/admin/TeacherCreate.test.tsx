import { beforeAll, describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import TeachersCreate from "./TeacherCreate";
import { apiUrl, server } from "@/test/server";
import type { Teacher } from "@/types/teacher";

// jsdom lacks the matchMedia the toaster calls.
beforeAll(() => {
  window.matchMedia = (query: string) =>
    ({ matches: false, media: query, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList;
});

const teacher: Teacher = {
  id: 7,
  firstName: "Theo",
  lastName: "Teacher",
  email: "theo@school.test",
  qualifications: "MSc Mathematics",
  subjectsTaught: "Math, Physics",
  availableHours: 20,
  schedulePreferences: "Morning",
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/admin/teachers/create"]}>
        <Routes>
          <Route path="/admin/teachers/create" element={<TeachersCreate />} />
          <Route path="/admin/teachers" element={<p>Teachers list</p>} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

async function fillAndSubmit() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("First Name"), "Theo");
  await user.type(screen.getByLabelText("Last Name"), "Teacher");
  await user.type(screen.getByLabelText("Email Address"), "theo@school.test");
  await user.type(screen.getByLabelText("Phone Number"), "+216 71 000 111");
  await user.type(screen.getByLabelText("Qualifications"), "MSc Mathematics");
  await user.type(screen.getByLabelText("Subjects Taught"), "Math, Physics");
  await user.type(screen.getByLabelText("Available Hours"), "20");
  await user.type(screen.getByLabelText("Schedule Preferences"), "Morning");
  await user.click(screen.getByRole("button", { name: "Create Teacher" }));
}

describe("TeachersCreate", () => {
  it("posts the teacher and returns to the teachers list", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/teachers"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: teacher }, { status: 201 });
      }),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText("Teachers list")).toBeInTheDocument();
    expect(await screen.findByText("Teacher created successfully!")).toBeInTheDocument();
    expect(body).toEqual({
      profile: {
        firstName: "Theo",
        lastName: "Teacher",
        email: "theo@school.test",
        telephone: "+216 71 000 111",
        birthday: "",
        address: "",
      },
      qualifications: "MSc Mathematics",
      subjectsTaught: "Math, Physics",
      availableHours: 20,
      schedulePreferences: "Morning",
    });
  });

  it("shows the backend error and stays on the form when creation fails", async () => {
    server.use(
      http.post(apiUrl("/admin/teachers"), () =>
        HttpResponse.json({ status: 409, detail: "Email already in use" }, { status: 409 }),
      ),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText("Email already in use")).toBeInTheDocument();
    expect(screen.queryByText("Teachers list")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create Teacher" })).toBeInTheDocument();
  });
});
