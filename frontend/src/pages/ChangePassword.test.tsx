import { afterEach, describe, expect, it } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import ChangePasswordPage from "./ChangePassword";
import { queryClient } from "@/lib/query-client";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { apiUrl, server } from "@/test/server";

const ada = { id: 1, email: "ada@fixtures.school.test", firstName: "Ada", lastName: "Student", role: "STUDENT" };

function renderSignedIn() {
  store.dispatch(loginSuccess({ user: ada, accessToken: "access-token", refreshToken: "refresh-token" }));
  queryClient.setQueryData(["userProfile"], ada);

  render(
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/change-password"]}>
          <Routes>
            <Route path="/change-password" element={<ChangePasswordPage />} />
            <Route path="/login" element={<p>Login page</p>} />
            <Route path="/" element={<p>Home page</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </Provider>,
  );
}

async function submitPasswords(oldPassword: string, newPassword: string) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/^Current Password/), oldPassword);
  await user.type(screen.getByLabelText(/^New Password/), newPassword);
  await user.type(screen.getByLabelText(/^Confirm New Password/), newPassword);
  await user.click(screen.getByRole("button", { name: "Change Password" }));
}

describe("ChangePasswordPage", () => {
  afterEach(() => {
    cleanup();
    store.dispatch(resetAuth());
    queryClient.clear();
  });

  it("ends the session and sends the user to sign in again after a successful change", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/auth/change-password"), async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 200 });
      }),
    );
    renderSignedIn();

    await submitPasswords("old-secret", "new-secret");

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(screen.queryByText("Home page")).not.toBeInTheDocument();
    expect(body).toEqual({ email: ada.email, oldPassword: "old-secret", newPassword: "new-secret" });
    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(localStorage.getItem("refreshToken")).toBeNull();
    expect(store.getState().auth).toEqual({ user: null, accessToken: null, refreshToken: null });
    expect(queryClient.getQueryData(["userProfile"])).toBeUndefined();
  });

  it("keeps the session when the current password is refused", async () => {
    server.use(
      http.post(apiUrl("/auth/change-password"), () =>
        HttpResponse.json(
          { type: "about:blank", title: "Bad Request", status: 400, detail: "Invalid current password" },
          { status: 400 },
        ),
      ),
    );
    renderSignedIn();

    await submitPasswords("wrong-secret", "new-secret");

    expect(await screen.findByRole("button", { name: "Change Password" })).toBeEnabled();
    expect(screen.queryByText("Login page")).not.toBeInTheDocument();
    expect(localStorage.getItem("accessToken")).toBe("access-token");
    expect(store.getState().auth.user?.email).toBe(ada.email);
  });
});
