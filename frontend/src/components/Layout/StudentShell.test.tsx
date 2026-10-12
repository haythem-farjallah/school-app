import { afterEach, describe, expect, it } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider } from "@tanstack/react-query";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { StudentShell } from "./StudentShell";
import { RequireAuth } from "@/components/guards/RequireAuth";
import { menuConfig } from "@/config/menuConfig";
import { queryClient } from "@/lib/query-client";
import { token } from "@/lib/token";
import { supportedLocales, type SupportedLocale } from "@/services/i18n";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { createTestI18n, localeBundles } from "@/test/i18n";

const ada = { id: 1, email: "ada@fixtures.school.test", firstName: "Ada", lastName: "Lovelace", role: "STUDENT" };

function signIn(user: Partial<typeof ada> = {}) {
  store.dispatch(
    loginSuccess({ user: { ...ada, ...user }, accessToken: "access-token", refreshToken: "refresh-token" }),
  );
}

function Where() {
  return <p data-testid="where">{useLocation().pathname}</p>;
}

function renderShell(path = "/student/dashboard", lng: SupportedLocale = "en") {
  return render(
    <I18nextProvider i18n={createTestI18n(lng)}>
      <Provider store={store}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[path]}>
            <Routes>
              <Route path="/login" element={<p>Login page</p>} />
              <Route
                element={
                  <RequireAuth>
                    <StudentShell />
                  </RequireAuth>
                }
              >
                <Route path="/" element={<Where />} />
                <Route path="/student/dashboard" element={<Where />} />
                <Route path="/student/announcements" element={<Where />} />
                <Route path="/profile" element={<Where />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </Provider>
    </I18nextProvider>,
  );
}

const topNavigation = () => screen.getByRole("navigation", { name: "Main navigation" });

afterEach(() => {
  store.dispatch(resetAuth());
  queryClient.clear();
});

describe("StudentShell navigation", () => {
  it("offers Home, Dashboard and Announcements with their own destinations", () => {
    signIn();
    renderShell();

    const nav = within(topNavigation());
    expect(nav.getAllByRole("link").map((link) => [link.textContent, link.getAttribute("href")])).toEqual([
      ["Home", "/"],
      ["Dashboard", "/student/dashboard"],
      ["Announcements", "/student/announcements"],
    ]);
  });

  it("is a top navigation: School App brand, one main landmark and no application sidebar", () => {
    signIn();
    renderShell();

    expect(screen.getByRole("banner")).toHaveTextContent("School App");
    expect(screen.getByRole("link", { name: "School App" })).toHaveAttribute("href", "/");
    expect(screen.getAllByRole("main")).toHaveLength(1);
    expect(screen.queryByRole("button", { name: /sidebar/i })).not.toBeInTheDocument();
    expect(document.querySelector("[data-sidebar]")).toBeNull();
    expect(screen.getByRole("link", { name: "Skip to main content" })).toHaveAttribute("href", "#main-content");
    expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
  });

  it("does not advertise timetable, courses or other unverified destinations", async () => {
    signIn();
    renderShell();
    const links = screen.getAllByRole("link").map((link) => link.textContent);
    await userEvent.setup().click(screen.getByRole("button", { name: "Open navigation menu" }));

    const advertised = [...links, ...screen.getAllByRole("menuitem").map((item) => item.textContent)].join("|");
    expect(advertised).not.toMatch(/timetable|schedule|assignment|course|exam|result|event|attendance|grade|learning/i);
    const hrefs = menuConfig.STUDENT.flatMap((section) => section.items)
      .flatMap((item) => ("href" in item ? [item.href] : []));
    expect(hrefs).toEqual(["/", "/student/dashboard", "/student/announcements", "/profile"]);
  });

  it.each([
    ["/", "Home"],
    ["/student/dashboard", "Dashboard"],
    ["/student/announcements", "Announcements"],
  ])("marks only the current page at %s", (path, current) => {
    signIn();
    renderShell(path);

    const links = within(topNavigation()).getAllByRole("link");
    expect(links.filter((link) => link.getAttribute("aria-current") === "page").map((link) => link.textContent)).toEqual([
      current,
    ]);
  });

  it("moves the current-page mark when another destination is opened", async () => {
    signIn();
    renderShell("/");

    await userEvent.setup().click(within(topNavigation()).getByRole("link", { name: "Announcements" }));

    expect(screen.getByTestId("where")).toHaveTextContent("/student/announcements");
    const nav = within(topNavigation());
    expect(nav.getByRole("link", { name: "Announcements" })).toHaveAttribute("aria-current", "page");
    expect(nav.getByRole("link", { name: "Home" })).not.toHaveAttribute("aria-current");
  });
});

describe("StudentShell account menu", () => {
  it("opens the current user's profile at /profile", async () => {
    signIn();
    renderShell();

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Account menu, Ada Lovelace" }));
    expect(screen.getByText("ada@fixtures.school.test")).toBeInTheDocument();
    await user.click(await screen.findByRole("menuitem", { name: "My Profile" }));

    expect(screen.getByTestId("where")).toHaveTextContent("/profile");
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("logs out through the session: tokens, account and cached data are cleared", async () => {
    signIn();
    queryClient.setQueryData(["private"], { secret: true });
    renderShell();

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Account menu, Ada Lovelace" }));
    await user.click(await screen.findByRole("menuitem", { name: "Log out" }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
    expect(store.getState().auth.user).toBeNull();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it("keeps a very long name inside the bar and the menu", async () => {
    const lastName = "Montgomery-Featherstonehaugh-Cholmondeley".repeat(3);
    signIn({ lastName, email: `${"long".repeat(30)}@fixtures.school.test` });
    renderShell();

    const trigger = screen.getByRole("button", { name: new RegExp(`Account menu, Ada ${lastName}`) });
    expect(within(trigger).getByText(`Ada ${lastName}`)).toHaveClass("truncate", "max-w-40");
    expect(screen.getByRole("banner").firstElementChild).toHaveClass("max-w-6xl", "w-full");

    await userEvent.setup().click(trigger);
    const menu = await screen.findByRole("menu");
    expect(menu).toHaveClass("max-w-[calc(100vw-2rem)]");
    expect(within(menu).getByText(`Ada ${lastName}`)).toHaveClass("truncate");
    expect(within(menu).getByText(/^long/)).toHaveClass("truncate");
  });
});

describe("StudentShell mobile menu", () => {
  it("opens from an accessible button, marks the current page and closes after choosing a destination", async () => {
    signIn();
    renderShell("/");

    const user = userEvent.setup();
    const button = screen.getByRole("button", { name: "Open navigation menu" });
    expect(button).toHaveAttribute("aria-expanded", "false");
    await user.click(button);

    const menu = await screen.findByRole("menu");
    expect(button).toHaveAttribute("aria-expanded", "true");
    expect(within(menu).getAllByRole("menuitem").map((item) => item.textContent)).toEqual([
      "Home",
      "Dashboard",
      "Announcements",
    ]);
    expect(within(menu).getByRole("menuitem", { name: "Home" })).toHaveAttribute("aria-current", "page");

    await user.click(within(menu).getByRole("menuitem", { name: "Announcements" }));

    expect(screen.getByTestId("where")).toHaveTextContent("/student/announcements");
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("is operable with the keyboard alone and returns focus to its button on Escape", async () => {
    signIn();
    renderShell("/");

    const user = userEvent.setup();
    const button = screen.getByRole("button", { name: "Open navigation menu" });
    await user.tab(); // skip link
    await user.tab(); // brand
    await user.tab(); // Home
    await user.tab(); // Dashboard
    await user.tab(); // Announcements
    await user.tab(); // menu button
    expect(button).toHaveFocus();

    await user.keyboard("{Enter}");
    expect(await screen.findByRole("menuitem", { name: "Home" })).toHaveFocus();
    await user.keyboard("{ArrowDown}{Enter}");
    expect(screen.getByTestId("where")).toHaveTextContent("/student/dashboard");

    await user.keyboard("{Enter}");
    await screen.findByRole("menu");
    await user.keyboard("{Escape}");
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
    expect(button).toHaveFocus();
  });
});

describe("StudentShell languages", () => {
  const expected = (locale: SupportedLocale) => {
    const navigation = (localeBundles[locale].navigation ?? {}) as Record<string, string>;
    return {
      links: [navigation.home, navigation.dashboard, navigation.announcements],
      profile: navigation.myProfile,
      logOut: navigation.logOut,
    };
  };

  it.each(supportedLocales)("%s shows translated labels and no raw keys", async (locale) => {
    signIn();
    const { container } = renderShell("/student/dashboard", locale);
    const { links, profile, logOut } = expected(locale);

    const nav = within(screen.getByRole("navigation", { name: (localeBundles[locale].shell as { navigationLabel: string }).navigationLabel }));
    expect(nav.getAllByRole("link").map((link) => link.textContent)).toEqual(links);

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /Ada Lovelace/ }));
    expect(await screen.findByRole("menuitem", { name: profile })).toBeInTheDocument();
    expect(screen.getByRole("menuitem", { name: logOut })).toBeInTheDocument();

    expect(container.textContent).not.toMatch(/\b(common|shell|navigation|roles|home)\.[a-zA-Z]/);
    expect(document.body.textContent).not.toMatch(/\bnavigation\.[a-zA-Z]/);
  });

  it("lays out Arabic with logical properties and opens menus from the inline end", async () => {
    signIn();
    const { container } = renderShell("/student/dashboard", "ar");

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /Ada Lovelace/ }));
    const menu = await screen.findByRole("menu");

    // Radix aligns physically: in right-to-left the inline end is "start".
    expect(menu).toHaveAttribute("data-align", "start");
    // No physical left/right spacing or positioning that would not mirror.
    const markup = `${container.innerHTML}${menu.outerHTML}`;
    expect(markup).not.toMatch(/class="[^"]*\s(ml|mr|pl|pr|left|right|text-left|text-right)-/);
  });

  it("opens English menus from the right edge", async () => {
    signIn();
    renderShell();

    await userEvent.setup().click(screen.getByRole("button", { name: /Ada Lovelace/ }));

    expect(await screen.findByRole("menu")).toHaveAttribute("data-align", "end");
  });
});
