import { beforeAll, describe, expect, it } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import StaffsCreate from "./StaffCreate";
import { apiUrl, server } from "@/test/server";

// jsdom lacks the pointer capture and scroll APIs the Radix Select calls, and the
// matchMedia the toaster calls.
beforeAll(() => {
  window.matchMedia = (query: string) =>
    ({ matches: false, media: query, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList;
  Element.prototype.hasPointerCapture = () => false;
  Element.prototype.releasePointerCapture = () => {};
  Element.prototype.scrollIntoView = () => {};
});

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/admin/staff/create"]}>
        <Routes>
          <Route path="/admin/staff/create" element={<StaffsCreate />} />
          <Route path="/admin/staff" element={<p>Staff list</p>} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

// The select labels are not bound to their triggers, so find each trigger next to its label.
async function choose(user: ReturnType<typeof userEvent.setup>, label: string, option: string) {
  await user.click(within(screen.getByText(label).parentElement!).getByRole("combobox"));
  await user.click(await screen.findByRole("option", { name: option }));
}

async function fillAndSubmit() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("First Name"), "Sara");
  await user.type(screen.getByLabelText("Last Name"), "Staff");
  await user.type(screen.getByLabelText("Email Address"), "sara@school.test");
  await choose(user, "Gender", "Female");
  await choose(user, "Department", "Finance");
  await user.click(screen.getByRole("button", { name: "Create Staff Member" }));
}

describe("StaffsCreate", () => {
  it("posts the staff member and returns to the staff list", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/staff"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Staff member created successfully", data: { id: 12 } }, { status: 201 });
      }),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText("Staff list")).toBeInTheDocument();
    expect(await screen.findByText("Staff member created successfully!")).toBeInTheDocument();
    expect(body).toEqual({
      profile: {
        firstName: "Sara",
        lastName: "Staff",
        email: "sara@school.test",
        telephone: "",
        gender: "F",
        address: "",
        role: "STAFF",
      },
      staffType: "ADMINISTRATIVE",
      department: "FINANCE",
    });
  });

  it("shows the backend error and stays on the form when creation fails", async () => {
    server.use(
      http.post(apiUrl("/admin/staff"), () =>
        HttpResponse.json({ status: 409, detail: "Email already in use" }, { status: 409 }),
      ),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText("Email already in use")).toBeInTheDocument();
    expect(screen.queryByText("Staff list")).not.toBeInTheDocument();
  });
});
