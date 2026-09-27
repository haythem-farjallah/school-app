import type { ReactNode } from "react";
import { render } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { configureStore } from "@reduxjs/toolkit";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import { Toaster } from "react-hot-toast";
import authReducer from "@/stores/authSlice";
import type { SupportedLocale } from "@/services/i18n";
import { createTestI18n } from "@/test/i18n";
import type { Student } from "@/types/student";

export const student: Student = {
  id: 5,
  firstName: "Sam",
  lastName: "Student",
  email: "sam@school.test",
  telephone: "+216 20 000 111",
  birthday: "2010-04-02",
  gender: "M",
  address: "1 School Road",
  gradeLevel: "HIGH",
  enrollmentYear: 2024,
};

/** jsdom lacks the pointer capture and scroll APIs Radix calls, and the matchMedia the toaster calls. */
export function installDomShims() {
  window.matchMedia = (query: string) =>
    ({ matches: false, media: query, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList;
  Element.prototype.hasPointerCapture = () => false;
  Element.prototype.releasePointerCapture = () => {};
  Element.prototype.scrollIntoView = () => {};
}

export function pageOf(content: Student[], { page = 0, size = 10, totalElements = content.length } = {}) {
  return { status: "success", data: { content, page, size, totalElements } };
}

interface RenderOptions {
  role?: "ADMIN" | "STAFF";
  lng?: SupportedLocale;
  /** Initial URL, e.g. "/staff/students?q=sam". The query seeds the URL state. */
  url: string;
  /** Route pattern the element is mounted at, e.g. "/:area/students/view/:id". */
  path: string;
  queryClient?: QueryClient;
}

/** Renders a students page the way the app mounts it: signed in, translated, routed and URL-state aware. */
export function renderStudentsRoute(element: ReactNode, { role = "ADMIN", lng = "en", url, path, queryClient }: RenderOptions) {
  const store = configureStore({
    reducer: { auth: authReducer },
    preloadedState: {
      auth: {
        user: { id: 1, email: "user@school.test", firstName: "Ada", lastName: "Admin", role },
        accessToken: "access-token",
        refreshToken: "refresh-token",
      },
    },
  });
  const client =
    queryClient ?? new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const i18n = createTestI18n(lng);
  // URL state (search, page) goes through nuqs; record each query string it writes.
  const urlQueries: string[] = [];

  const result = render(
    <Provider store={store}>
      <I18nextProvider i18n={i18n}>
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={[url]}>
            <NuqsTestingAdapter
              searchParams={url.split("?")[1] ?? ""}
              onUrlUpdate={(event) => urlQueries.push(event.queryString)}
              rateLimitFactor={0}
            >
              <Routes>
                <Route path={path} element={element} />
                {/* Where navigation away from the page under test lands. */}
                <Route path="/admin/students" element={<p>Admin students list</p>} />
                <Route path="/staff/students" element={<p>Staff students list</p>} />
                <Route path="*" element={<p>Other page</p>} />
              </Routes>
            </NuqsTestingAdapter>
          </MemoryRouter>
          <Toaster />
        </QueryClientProvider>
      </I18nextProvider>
    </Provider>,
  );
  return { ...result, queryClient: client, i18n, lastUrlQuery: () => urlQueries.at(-1) };
}
