import { beforeAll, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { http, HttpResponse } from "msw";
import { NotificationCenter } from "./NotificationCenter";
import { store } from "@/stores/store";
import { apiUrl, server } from "@/test/server";

const unread = {
  id: 31,
  title: "Absence recorded",
  message: "Sam was absent in Mathematics on Monday.",
  type: "ATTENDANCE_MARKED",
  entityType: "Attendance",
  entityId: 3,
  actionUrl: null,
  readStatus: false,
  createdAt: "2026-09-21T08:30:00",
  readAt: null,
};

function page(content: unknown[]) {
  return { status: "success", data: { content, page: 0, size: 20, totalElements: content.length } };
}

function renderCenter() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <NotificationCenter userId={5} isOpen onClose={() => {}} />
      </QueryClientProvider>
    </Provider>,
  );
}

// The scroll area and the action buttons' tooltips measure elements; jsdom has no ResizeObserver.
beforeAll(() => {
  vi.stubGlobal(
    "ResizeObserver",
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  );
});

describe("NotificationCenter", () => {
  it("lists the persisted notifications of the current user with their message", async () => {
    const readStatusParams: (string | null)[] = [];
    server.use(
      http.get(apiUrl("/v1/notifications"), ({ request }) => {
        readStatusParams.push(new URL(request.url).searchParams.get("readStatus"));
        return HttpResponse.json(page([unread]));
      }),
    );
    renderCenter();

    expect(await screen.findByText("Absence recorded")).toBeInTheDocument();
    expect(screen.getByText("Sam was absent in Mathematics on Monday.")).toBeInTheDocument();
    expect(screen.getByText("1 unread")).toBeInTheDocument();
    // The full list and the unread list (readStatus=false) are both read.
    expect(readStatusParams).toEqual(expect.arrayContaining([null, "false"]));
  });

  it("marks a notification as read through PATCH /v1/notifications/{id}/read", async () => {
    let marked = false;
    server.use(
      http.get(apiUrl("/v1/notifications"), () => HttpResponse.json(page(marked ? [] : [unread]))),
      http.patch(apiUrl("/v1/notifications/31/read"), () => {
        marked = true;
        return HttpResponse.json({ status: "success", data: { ...unread, readStatus: true } });
      }),
    );
    renderCenter();

    await userEvent.click(await screen.findByTitle("Mark as read"));

    await waitFor(() => expect(marked).toBe(true));
    expect(await screen.findByText("No notifications")).toBeInTheDocument();
  });
});
