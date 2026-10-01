import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { EnrollmentActions } from "./enrollment-actions";
import { EnrollmentStatus, type Enrollment } from "@/types/enrollment";
import { apiUrl, server } from "@/test/server";

function enrollment(status: EnrollmentStatus): Enrollment {
  return {
    id: 12,
    studentName: "Sam Student",
    studentEmail: "sam@school.test",
    className: "7A",
    gradeCount: 0,
    enrolledAt: "2026-09-01",
    status,
    studentId: 5,
    classId: 4,
  };
}

async function openMenu(status: EnrollmentStatus, onSuccess?: () => void) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <EnrollmentActions enrollment={enrollment(status)} onSuccess={onSuccess} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  await userEvent.click(screen.getByRole("button", { name: /open menu/i }));
}

describe("EnrollmentActions", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("offers withdrawal, status editing and transfer for an active enrollment", async () => {
    await openMenu(EnrollmentStatus.ACTIVE);

    expect(await screen.findByText("Withdraw Enrollment")).toBeInTheDocument();
    expect(screen.getByText("Edit Status")).toBeInTheDocument();
    expect(screen.getByText("Transfer Student")).toBeInTheDocument();
    expect(screen.queryByText(/drop/i)).not.toBeInTheDocument();
  });

  it.each([EnrollmentStatus.COMPLETED, EnrollmentStatus.TRANSFERRED, EnrollmentStatus.WITHDRAWN])(
    "offers nothing that would reactivate a %s enrollment",
    async (status) => {
      await openMenu(status);

      expect(await screen.findByText("View Details")).toBeInTheDocument();
      expect(screen.queryByText("Withdraw Enrollment")).not.toBeInTheDocument();
      expect(screen.queryByText("Edit Status")).not.toBeInTheDocument();
      expect(screen.queryByText("Transfer Student")).not.toBeInTheDocument();
    },
  );

  it("withdraws with the entered reason", async () => {
    let body: unknown;
    server.use(
      http.delete(apiUrl("/v1/enrollments/12"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "Student withdrawn from enrollment successfully", data: "" });
      }),
    );
    vi.spyOn(window, "prompt").mockReturnValue("Moved abroad");
    const success = vi.spyOn(toast, "success");
    const onSuccess = vi.fn();
    await openMenu(EnrollmentStatus.ACTIVE, onSuccess);

    await userEvent.click(await screen.findByText("Withdraw Enrollment"));

    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
    expect(body).toEqual({ reason: "Moved abroad" });
    expect(success).toHaveBeenCalledWith("Student withdrawn from enrollment successfully");
  });
});
