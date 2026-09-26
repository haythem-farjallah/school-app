import { afterEach, describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { http, HttpResponse } from "msw";
import { useDashboardStats } from "./use-dashboard-stats";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { apiUrl, server } from "@/test/server";

const admin = { id: 1, email: "admin@fixtures.school.test", firstName: "Ada", lastName: "Admin", role: "ADMIN" };
const systemStats = {
  totalStudents: 120,
  totalTeachers: 14,
  totalParents: 90,
  totalClasses: 6,
  totalCourses: 11,
  activeEnrollments: 118,
  systemHealth: 98.5,
  serverStatus: "ONLINE",
};

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </Provider>
  );
}

describe("useDashboardStats", () => {
  afterEach(() => {
    store.dispatch(resetAuth());
  });

  it("reads the system statistics of the admin dashboard", async () => {
    store.dispatch(loginSuccess({ user: admin, accessToken: "access-token", refreshToken: "refresh-token" }));
    server.use(
      http.get(apiUrl("/v1/dashboard/admin/1"), () =>
        HttpResponse.json({ status: "success", data: { type: "ADMIN", systemStats } }),
      ),
    );
    const { result } = renderHook(() => useDashboardStats(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(systemStats);
  });

  it("reports a failed dashboard request as an error instead of substitute counts", async () => {
    store.dispatch(loginSuccess({ user: admin, accessToken: "access-token", refreshToken: "refresh-token" }));
    server.use(
      http.get(apiUrl("/v1/dashboard/admin/1"), () =>
        HttpResponse.json({ title: "Internal Server Error", status: 500 }, { status: 500 }),
      ),
    );
    const { result } = renderHook(() => useDashboardStats(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });
});
