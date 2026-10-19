import {
  act,
  cleanup,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";
import { delay, http, HttpResponse } from "msw";
import StudentDashboard from "./Dashboard";
import { queryClient } from "@/lib/query-client";
import { terminateSession } from "@/lib/session";
import { store } from "@/stores/store";
import { loginSuccess } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";
import { apiUrl, server } from "@/test/server";

const alice = {
  id: 21,
  firstName: "Alice",
  lastName: "Student",
  email: "alice@fixtures.school.test",
  role: "STUDENT",
};
const bob = {
  ...alice,
  id: 22,
  firstName: "Bob",
  email: "bob@fixtures.school.test",
};
const dashboard = {
  type: "STUDENT",
  baseInfo: {
    userInfo: {
      id: 21,
      name: "Alice Student",
      email: alice.email,
      role: "STUDENT",
      avatarUrl: null,
    },
    notifications: [
      {
        id: 6,
        title: "Result available",
        message: "Your result is ready.",
        type: "INFO",
        isRead: false,
        createdAt: "2026-10-01T10:00:00",
        actionUrl: null,
      },
    ],
    recentActivities: [],
    lastLogin: null,
  },
  stats: {
    totalEnrollments: 7,
    averageGrade: 14,
    completedCourses: null,
    totalAssignments: null,
    currentGPA: "N/A",
    academicStanding: "N/A",
  },
  recentGrades: [
    {
      courseName: "Historical class B",
      score: 14 as number | null,
      content: "Term assessment",
      gradedAt: "2026-10-01T09:00:00" as string | null,
    },
  ],
  enrolledClasses: [
    {
      classId: 4,
      className: "Current class A",
      teacherName: "Theo Teacher",
      totalStudents: 24,
      schedule: "",
    },
  ],
  upcomingEvents: [],
};
const announcement = {
  id: 9,
  title: "School notice",
  body: "The library opens at nine.",
  importance: "HIGH",
  isPublic: true,
  createdAt: "2026-09-30T08:00:00",
  startDate: "2026-10-01T08:00:00",
  endDate: "2026-10-30T18:00:00",
};

function serveDashboard(data = dashboard) {
  server.use(
    http.get(apiUrl("/v1/dashboard/current-user"), ({ request }) => {
      expect(request.headers.get("Authorization")).toBe("Bearer alice-token");
      return HttpResponse.json({ status: "success", data });
    }),
  );
}

function renderDashboard() {
  return render(
    <HelmetProvider>
      <I18nextProvider i18n={createTestI18n()}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter>
              <StudentDashboard />
            </MemoryRouter>
          </QueryClientProvider>
        </Provider>
      </I18nextProvider>
    </HelmetProvider>,
  );
}

beforeEach(() => {
  store.dispatch(
    loginSuccess({
      user: alice,
      accessToken: "alice-token",
      refreshToken: "alice-refresh",
    }),
  );
  serveDashboard();
  server.use(
    http.get(apiUrl("/v1/announcements/public"), ({ request }) => {
      expect(new URL(request.url).searchParams.toString()).toBe(
        "page=0&size=3",
      );
      return HttpResponse.json({
        data: { content: [announcement], page: 0, size: 3, totalElements: 1 },
      });
    }),
  );
});

afterEach(() => {
  cleanup();
  terminateSession();
});

describe("student dashboard real data", () => {
  it("renders authenticated identity, current enrollment, raw recorded scores and real notifications", async () => {
    renderDashboard();
    expect(await screen.findByText("Alice Student")).toBeInTheDocument();
    expect(screen.getByText(alice.email)).toBeInTheDocument();
    expect(screen.getByText("Current class A")).toBeInTheDocument();
    const average = screen.getByLabelText("Recorded grade average");
    expect(average).toHaveTextContent("14");
    expect(average).not.toHaveTextContent("%");
    expect(screen.getByText("Term assessment")).toBeInTheDocument();
    expect(screen.getByText("Class: Historical class B")).toBeInTheDocument();
    expect(screen.getByText("Your result is ready.")).toBeInTheDocument();
    expect(
      document.querySelector('time[datetime="2026-10-01T09:00:00"]'),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(
        /85\.4|92%|Midterm Schedule Released|Library Hours Extended|Science Fair Registration|Physics lab report|4A - Physics|Student Name|student@school.edu|Last login/i,
      ),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(
        /Assignments Due|Attendance Rate|Upcoming Exams|Course:/,
      ),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", {
        name: /Assignments|Attendance|Schedule|Learning Resources|Grades/,
      }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Profile" })).toHaveAttribute(
      "href",
      "/student/profile",
    );
  });

  it("renders actual public announcement fields and links without claiming globally latest ordering", async () => {
    renderDashboard();
    expect(await screen.findByText("School notice")).toBeInTheDocument();
    expect(screen.getByText(announcement.body)).toBeInTheDocument();
    expect(screen.getByText("HIGH")).toBeInTheDocument();
    for (const date of [
      announcement.createdAt,
      announcement.startDate,
      announcement.endDate,
    ]) {
      expect(
        document.querySelector(`time[datetime="${date}"]`),
      ).toBeInTheDocument();
    }
    expect(
      screen.getByRole("link", { name: "View all announcements" }),
    ).toHaveAttribute("href", "/student/announcements");
    expect(screen.queryByText(/latest/i)).not.toBeInTheDocument();
  });

  it.each([
    { recentGrades: [] },
    { recentGrades: [{ ...dashboard.recentGrades[0], score: null }] },
  ])(
    "does not treat the backend zero default as recorded performance (%j)",
    async ({ recentGrades }) => {
      serveDashboard({
        ...dashboard,
        stats: { ...dashboard.stats, averageGrade: 0 },
        recentGrades,
      });
      renderDashboard();
      expect(
        await screen.findByLabelText("Recorded grade average"),
      ).toHaveTextContent("Unavailable");
      expect(screen.queryByText(/^0%?$/)).not.toBeInTheDocument();
    },
  );

  it("preserves a legitimate recorded score of zero", async () => {
    serveDashboard({
      ...dashboard,
      stats: { ...dashboard.stats, averageGrade: 0 },
      recentGrades: [{ ...dashboard.recentGrades[0], score: 0 }],
    });
    renderDashboard();
    expect(
      await screen.findByLabelText("Recorded grade average"),
    ).toHaveTextContent(/^0$/);
    expect(screen.getByLabelText("Recorded score")).toHaveTextContent(/^0$/);
  });

  it("shows honest empty states and missing result fields", async () => {
    serveDashboard({
      ...dashboard,
      enrolledClasses: [],
      recentGrades: [],
      baseInfo: { ...dashboard.baseInfo, notifications: [] },
    });
    server.use(
      http.get(apiUrl("/v1/announcements/public"), () =>
        HttpResponse.json({
          data: { content: [], page: 0, size: 3, totalElements: 0 },
        }),
      ),
    );
    renderDashboard();
    expect(
      await screen.findByText("No current enrollment available."),
    ).toBeInTheDocument();
    expect(screen.getByText("No results available.")).toBeInTheDocument();
    expect(screen.getByText("No unread notifications.")).toBeInTheDocument();
    expect(
      await screen.findByText("No public announcements available."),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Timetable information is not available on this dashboard yet.",
      ),
    ).toBeInTheDocument();
  });

  it("does not invent a score, class or date for an incomplete result", async () => {
    serveDashboard({
      ...dashboard,
      recentGrades: [
        {
          courseName: "",
          content: "Pending assessment",
          score: null,
          gradedAt: null,
        },
      ],
    });
    renderDashboard();
    expect(await screen.findByText("Pending assessment")).toBeInTheDocument();
    expect(screen.getByLabelText("Recorded score")).toHaveTextContent(
      "Not recorded",
    );
    expect(screen.queryByText(/Course:/)).not.toBeInTheDocument();
    await screen.findByText("School notice");
    expect(document.querySelectorAll("time")).toHaveLength(4); // Only actual notification/announcement dates.
  });

  it.each([403, 404, 500, "network"])(
    "shows dashboard failure (%s), keeps announcements usable, and retries",
    async (status) => {
      server.use(
        http.get(apiUrl("/v1/dashboard/current-user"), () =>
          status === "network"
            ? HttpResponse.error()
            : new HttpResponse(null, { status: Number(status) }),
        ),
      );
      renderDashboard();
      expect(await screen.findByRole("alert")).toHaveTextContent(
        "Unable to load your dashboard.",
      );
      expect(await screen.findByText("School notice")).toBeInTheDocument();
      expect(
        screen.queryByText("No results available."),
      ).not.toBeInTheDocument();
      serveDashboard();
      await userEvent.click(screen.getByRole("button", { name: "Retry" }));
      expect(await screen.findByText("Current class A")).toBeInTheDocument();
    },
  );

  it("keeps dashboard usable when announcements fail and retries announcements independently", async () => {
    server.use(
      http.get(
        apiUrl("/v1/announcements/public"),
        () => new HttpResponse(null, { status: 500 }),
      ),
    );
    renderDashboard();
    expect(await screen.findByText("Current class A")).toBeInTheDocument();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to load public announcements.",
    );
    server.use(
      http.get(apiUrl("/v1/announcements/public"), () =>
        HttpResponse.json({ data: { content: [announcement] } }),
      ),
    );
    await userEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(await screen.findByText("School notice")).toBeInTheDocument();
  });

  it("distinguishes both loading sections from empty success", async () => {
    server.use(
      http.get(apiUrl("/v1/dashboard/current-user"), async () => {
        await delay(150);
        return HttpResponse.json({ data: dashboard });
      }),
      http.get(apiUrl("/v1/announcements/public"), async () => {
        await delay(150);
        return HttpResponse.json({ data: { content: [] } });
      }),
    );
    renderDashboard();
    expect(screen.getByText("Loading your dashboard…")).toBeInTheDocument();
    expect(
      screen.getByText("Loading public announcements…"),
    ).toBeInTheDocument();
    expect(screen.queryByText("No results available.")).not.toBeInTheDocument();
    expect(await screen.findByText("Current class A")).toBeInTheDocument();
  });

  it("isolates cached account data on a direct user switch, then clears it on logout", async () => {
    renderDashboard();
    expect(await screen.findByText("Current class A")).toBeInTheDocument();
    expect(await screen.findByText("School notice")).toBeInTheDocument();
    server.use(
      http.get(apiUrl("/v1/dashboard/current-user"), async ({ request }) => {
        expect(request.headers.get("Authorization")).toBe("Bearer bob-token");
        await delay(100);
        return HttpResponse.json({
          data: {
            ...dashboard,
            baseInfo: {
              ...dashboard.baseInfo,
              userInfo: {
                ...dashboard.baseInfo.userInfo,
                id: bob.id,
                name: "Bob Student",
                email: bob.email,
              },
            },
            enrolledClasses: [],
          },
        });
      }),
      http.get(apiUrl("/v1/announcements/public"), () =>
        HttpResponse.json({ data: { content: [] } }),
      ),
    );
    act(() =>
      store.dispatch(
        loginSuccess({
          user: bob,
          accessToken: "bob-token",
          refreshToken: "bob-refresh",
        }),
      ),
    );
    expect(screen.queryByText("Alice Student")).not.toBeInTheDocument();
    expect(screen.queryByText("Current class A")).not.toBeInTheDocument();
    expect(screen.queryByText("School notice")).not.toBeInTheDocument();
    expect(await screen.findByText("Bob Student")).toBeInTheDocument();
    act(terminateSession);
    expect(screen.queryByText("Bob Student")).not.toBeInTheDocument();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it("ends the session on a rejected 401 refresh instead of rendering empty-success data", async () => {
    server.use(
      http.get(
        apiUrl("/v1/dashboard/current-user"),
        () => new HttpResponse(null, { status: 401 }),
      ),
      http.post(
        apiUrl("/auth/refresh"),
        () => new HttpResponse(null, { status: 401 }),
      ),
    );
    const view = renderDashboard();
    await waitFor(() => expect(store.getState().auth.user).toBeNull());
    expect(
      within(view.container).queryByText("No results available."),
    ).not.toBeInTheDocument();
    expect(view.container).toBeEmptyDOMElement();
  });

  it("rejects an unexpected dashboard identity without exposing its records", async () => {
    serveDashboard({
      ...dashboard,
      baseInfo: {
        ...dashboard.baseInfo,
        userInfo: { ...dashboard.baseInfo.userInfo, id: bob.id },
      },
    });
    renderDashboard();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to load your dashboard.",
    );
    expect(screen.queryByText("Current class A")).not.toBeInTheDocument();
  });

  it("does not restore private data when a request completes after logout", async () => {
    let release: () => void = () => {};
    const pending = new Promise<void>((resolve) => {
      release = resolve;
    });
    let started = false;
    server.use(
      http.get(apiUrl("/v1/dashboard/current-user"), async () => {
        started = true;
        await pending;
        return HttpResponse.json({ data: dashboard });
      }),
    );
    const view = renderDashboard();
    await waitFor(() => expect(started).toBe(true));
    act(terminateSession);
    await act(async () => {
      release();
      await pending;
    });
    expect(view.container).toBeEmptyDOMElement();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });
});
