import { beforeAll, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "react-hot-toast";
import type { ReactNode } from "react";
import { AddStaffSheet, EditStaffSheet } from "./staff-sheet";
import { apiUrl, server } from "@/test/server";
import { StaffType, type Staff } from "@/types/staff";

// jsdom lacks the pointer capture and scroll APIs the Radix Select calls, and the
// matchMedia the toaster calls.
beforeAll(() => {
  window.matchMedia = (query: string) =>
    ({ matches: false, media: query, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList;
  Element.prototype.hasPointerCapture = () => false;
  Element.prototype.releasePointerCapture = () => {};
  Element.prototype.scrollIntoView = () => {};
});

const staff: Staff = {
  id: 12,
  firstName: "Sara",
  lastName: "Staff",
  email: "sara@school.test",
  telephone: "555-0200",
  birthday: "1985-06-15",
  gender: "F",
  address: "3 Office Lane",
  staffType: StaffType.ADMINISTRATIVE,
  department: "FINANCE",
  createdAt: "2026-01-10T08:00:00Z",
  updatedAt: "2026-01-10T08:00:00Z",
};

function renderWithClient(ui: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      {ui}
      <Toaster />
    </QueryClientProvider>,
  );
}

// The select labels are not bound to their triggers, so find each trigger next to its label.
async function choose(user: ReturnType<typeof userEvent.setup>, sheet: HTMLElement, label: string, option: string) {
  await user.click(within(within(sheet).getByText(label).parentElement!).getByRole("combobox"));
  await user.click(await screen.findByRole("option", { name: option }));
}

describe("AddStaffSheet", () => {
  // Staff type keeps the sheet's default (Administrative).
  async function openFillAndSubmit() {
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Add Staff Member" }));
    const sheet = await screen.findByRole("dialog");
    await user.type(within(sheet).getByLabelText("First Name"), "Sara");
    await user.type(within(sheet).getByLabelText("Last Name"), "Staff");
    await user.type(within(sheet).getByLabelText("Email Address"), "sara@school.test");
    await choose(user, sheet, "Gender", "Female");
    await choose(user, sheet, "Department", "Finance");
    await user.click(within(sheet).getByRole("button", { name: "Add Staff Member" }));
  }

  it("posts the staff member with the nested profile, closes the sheet and reports success", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/staff"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Staff member created successfully", data: staff }, { status: 201 });
      }),
    );
    const onSuccess = vi.fn();
    renderWithClient(<AddStaffSheet onSuccess={onSuccess} />);

    await openFillAndSubmit();

    expect(await screen.findByText("Staff member added successfully!")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(body).toEqual({
      profile: {
        firstName: "Sara",
        lastName: "Staff",
        email: "sara@school.test",
        telephone: "",
        birthday: "",
        gender: "F",
        address: "",
        role: "STAFF",
      },
      staffType: "ADMINISTRATIVE",
      department: "FINANCE",
    });
  });

  it("shows the backend error and keeps the sheet open when creation fails", async () => {
    server.use(
      http.post(apiUrl("/admin/staff"), () =>
        HttpResponse.json({ status: 409, detail: "Email already in use" }, { status: 409 }),
      ),
    );
    const onSuccess = vi.fn();
    renderWithClient(<AddStaffSheet onSuccess={onSuccess} />);

    await openFillAndSubmit();

    expect(await screen.findByText("Email already in use")).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });
});

describe("EditStaffSheet", () => {
  async function openEditAndSubmit() {
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Edit" }));
    const sheet = await screen.findByRole("dialog");
    const address = within(sheet).getByLabelText("Address");
    await user.clear(address);
    await user.type(address, "4 Server Road");
    await choose(user, sheet, "Department", "Information Technology");
    await user.click(within(sheet).getByRole("button", { name: "Update Staff Member" }));
  }

  it("patches the staff member with the profile nested as the backend update expects", async () => {
    let body: unknown;
    server.use(
      http.patch(apiUrl("/admin/staff/12"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({
          status: "Staff member updated successfully",
          data: { ...staff, address: "4 Server Road", department: "IT" },
        });
      }),
    );
    const onSuccess = vi.fn();
    renderWithClient(<EditStaffSheet staff={staff} onSuccess={onSuccess} />);

    await openEditAndSubmit();

    expect(await screen.findByText("Staff member updated successfully!")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(body).toEqual({
      profile: {
        firstName: "Sara",
        lastName: "Staff",
        email: "sara@school.test",
        telephone: "555-0200",
        birthday: "1985-06-15",
        gender: "F",
        address: "4 Server Road",
      },
      staffType: "ADMINISTRATIVE",
      department: "IT",
    });
  });

  it("shows the backend error and keeps the sheet open when the update fails", async () => {
    server.use(
      http.patch(apiUrl("/admin/staff/12"), () =>
        HttpResponse.json({ status: 404, detail: "Staff member not found" }, { status: 404 }),
      ),
    );
    const onSuccess = vi.fn();
    renderWithClient(<EditStaffSheet staff={staff} onSuccess={onSuccess} />);

    await openEditAndSubmit();

    expect(await screen.findByText("Staff member not found")).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });
});
