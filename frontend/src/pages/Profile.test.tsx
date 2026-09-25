import { beforeEach, describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { configureStore } from "@reduxjs/toolkit";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";
import ProfilePage from "./Profile";
import authReducer from "@/stores/authSlice";
import type { ProfileSettings, UserProfile } from "@/features/auth/profileSettings";
import { token } from "@/lib/token";
import { apiUrl, server } from "@/test/server";

const teacher: UserProfile = {
  id: 10,
  firstName: "Theo",
  lastName: "Teacher",
  email: "teacher@fixtures.school.test",
  role: "TEACHER",
  telephone: "+216 71 000 111",
  address: "12 Rue de Marseille, Tunis",
  profileTheme: null,
  profileLanguage: null,
  permissions: [],
};

const settings: ProfileSettings = {
  language: "en",
  theme: "light",
  notificationsEnabled: true,
  darkMode: false,
};

function renderProfilePage() {
  const store = configureStore({
    reducer: { auth: authReducer },
    preloadedState: {
      auth: {
        user: { id: teacher.id, email: teacher.email, firstName: teacher.firstName, lastName: teacher.lastName, role: teacher.role },
        accessToken: "access-token",
        refreshToken: "refresh-token",
      },
    },
  });
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });

  render(
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfilePage />
        </MemoryRouter>
      </QueryClientProvider>
    </Provider>,
  );
}

describe("ProfilePage", () => {
  beforeEach(() => {
    token.access = "access-token";
    server.use(http.get(apiUrl("/me/settings"), () => HttpResponse.json(settings)));
  });

  it("shows the contact details returned by the profile endpoint", async () => {
    server.use(http.get(apiUrl("/me/profile"), () => HttpResponse.json(teacher)));
    renderProfilePage();

    expect(await screen.findByText("+216 71 000 111")).toBeInTheDocument();
    expect(screen.getByText("12 Rue de Marseille, Tunis")).toBeInTheDocument();
    expect(screen.getByLabelText("Phone Number")).toHaveValue("+216 71 000 111");
    expect(screen.getByLabelText("Address")).toHaveValue("12 Rue de Marseille, Tunis");
  });

  it("shows missing contact details as not provided", async () => {
    server.use(http.get(apiUrl("/me/profile"), () => HttpResponse.json({ ...teacher, telephone: null, address: null })));
    renderProfilePage();

    expect(await screen.findAllByText("Not provided")).toHaveLength(2);
    expect(screen.getByLabelText("Phone Number")).toHaveValue("");
    expect(screen.getByLabelText("Address")).toHaveValue("");
  });

  it("saves edited contact details and shows the values the server returned", async () => {
    let current = teacher;
    let body: unknown;
    server.use(
      http.get(apiUrl("/me/profile"), () => HttpResponse.json(current)),
      http.patch(apiUrl("/me/profile"), async ({ request }) => {
        body = await request.json();
        current = { ...current, ...(body as Partial<UserProfile>) };
        return HttpResponse.json(current);
      }),
    );
    renderProfilePage();
    const user = userEvent.setup();

    const phone = await screen.findByLabelText("Phone Number");
    await user.clear(phone);
    await user.type(phone, "+216 98 765 432");
    await user.click(screen.getByRole("button", { name: "Update Contact Info" }));

    expect(await screen.findByText("+216 98 765 432")).toBeInTheDocument();
    expect(body).toEqual({ telephone: "+216 98 765 432", address: "12 Rue de Marseille, Tunis" });
    await waitFor(() => expect(screen.getByLabelText("Phone Number")).toHaveValue("+216 98 765 432"));
  });
});
