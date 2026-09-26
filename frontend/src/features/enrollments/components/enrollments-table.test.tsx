import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { EnrollmentsTable } from "./enrollments-table";
import { TooltipProvider } from "@/components/ui/tooltip";
import { apiUrl, server } from "@/test/server";

describe("EnrollmentsTable auto-enrollment", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("reports the counts of the auto-enrollment result", async () => {
    const success = vi.spyOn(toast, "success");
    const failure = vi.spyOn(toast, "error");
    server.use(
      http.get(apiUrl("/v1/enrollments"), () =>
        HttpResponse.json({ status: "success", data: { content: [], page: 0, size: 10, totalElements: 0 } }),
      ),
      http.post(apiUrl("/v1/enrollments/auto-enroll"), () =>
        HttpResponse.json({
          status: "Auto-enrollment completed",
          data: { success: true, studentsEnrolled: 12, classesUsed: 3, classesCreated: 1, errors: [], isPreview: false },
        }),
      ),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NuqsTestingAdapter>
            <TooltipProvider>
              <EnrollmentsTable />
            </TooltipProvider>
          </NuqsTestingAdapter>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const [autoEnroll] = await screen.findAllByRole("button", { name: /Auto-Enrollment/ });
    await userEvent.click(autoEnroll);

    await waitFor(() =>
      expect(success).toHaveBeenCalledWith(
        "Auto-Enrollment Complete! Successfully enrolled 12 students into 3 classes (1 new classes created)",
      ),
    );
    expect(failure).not.toHaveBeenCalled();
  });
});
