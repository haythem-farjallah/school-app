import { describe, expect, it } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { http, HttpResponse } from "msw";
import { ParentsTable } from "./parents-table";
import { TooltipProvider } from "@/components/ui/tooltip";
import { apiUrl, server } from "@/test/server";

const parent = {
  id: 9,
  firstName: "Pat",
  lastName: "Parent",
  email: "pat@family.test",
  telephone: "555-0300",
  preferredContactMethod: "EMAIL",
  relation: "Mother",
  children: [],
};

function renderTable() {
  server.use(
    http.get(apiUrl("/admin/parent-management"), () =>
      HttpResponse.json({ status: "success", data: { content: [parent], page: 0, size: 10, totalElements: 1 } }),
    ),
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <NuqsTestingAdapter>
          <TooltipProvider>
            <ParentsTable />
          </TooltipProvider>
        </NuqsTestingAdapter>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ParentsTable", () => {
  it("offers no parent import, since there is no import endpoint", async () => {
    renderTable();

    await screen.findByText("pat@family.test");
    expect(screen.queryByRole("button", { name: /import/i })).not.toBeInTheDocument();
  });

  it("offers only bulk delete for selected parents", async () => {
    const user = userEvent.setup();
    renderTable();

    await user.click(await screen.findByRole("checkbox", { name: "Select row" }));

    const deleteAction = await screen.findByRole("button", { name: "Delete (1)" });
    const actionBar = deleteAction.closest<HTMLElement>('[role="toolbar"]')!;
    expect(within(actionBar).queryByRole("button", { name: /status/i })).not.toBeInTheDocument();
    expect(within(actionBar).queryByRole("button", { name: /export/i })).not.toBeInTheDocument();
    expect(within(actionBar).queryByRole("button", { name: /email/i })).not.toBeInTheDocument();
  });
});
