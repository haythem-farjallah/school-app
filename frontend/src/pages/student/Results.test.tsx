import { act, cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { MemoryRouter, useLocation } from "react-router-dom";
import { delay, http, HttpResponse } from "msw";
import StudentResults from "./Results";
import type { StudentResult } from "@/features/grades/types";
import { queryClient } from "@/lib/query-client";
import { terminateSession } from "@/lib/session";
import { store } from "@/stores/store";
import { loginSuccess } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";
import { apiUrl, server } from "@/test/server";

const alice = { id: 21, firstName: "Alice", lastName: "Student", email: "alice@fixtures.school.test", role: "STUDENT" };
const bob = { ...alice, id: 22, firstName: "Bob", email: "bob@fixtures.school.test" };

function result(id: number, overrides: Partial<StudentResult> = {}): StudentResult {
  return {
    id,
    content: `Assessment ${id}`,
    score: 12,
    gradedAt: "2026-10-01T09:00:00",
    studentId: alice.id,
    classId: 4,
    className: "Class 7A",
    teacherId: 9,
    teacherName: "Theo Teacher",
    ...overrides,
  };
}

function page(content: StudentResult[], pageNumber = 0, totalElements = content.length, size = 10) {
  return { status: "success", data: { content, page: pageNumber, size, totalElements } };
}

/** Serves the grades of one student and records the query string of every request. */
function serveGrades(studentId: number, respond: (params: URLSearchParams) => Response | Promise<Response>) {
  const requests: URLSearchParams[] = [];
  server.use(
    http.get(apiUrl(`/v1/grades/student/${studentId}/paged`), ({ request }) => {
      const params = new URL(request.url).searchParams;
      requests.push(params);
      return respond(params);
    }),
  );
  return requests;
}

function CurrentLocation() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname + location.search}</div>;
}

function renderPage(initialEntry = "/student/results") {
  return render(
    <HelmetProvider>
      <I18nextProvider i18n={createTestI18n()}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialEntry]}>
              <StudentResults />
              <CurrentLocation />
            </MemoryRouter>
          </QueryClientProvider>
        </Provider>
      </I18nextProvider>
    </HelmetProvider>,
  );
}

function logIn(user: typeof alice, accessToken = "alice-token") {
  store.dispatch(loginSuccess({ user, accessToken, refreshToken: `${accessToken}-refresh` }));
}

beforeEach(() => logIn(alice));

afterEach(() => {
  cleanup();
  terminateSession();
});

describe("student results", () => {
  it("shows the recorded fields of the student's own results, newest first, without invented values", async () => {
    const requests = serveGrades(alice.id, () =>
      HttpResponse.json(
        page([
          result(1, { content: "Term exam", score: 14.5, className: "Class 7A", teacherName: "Theo Teacher" }),
          result(2, { content: "Quiz", score: 8, className: "Class 7B", teacherName: "Tina Teacher", gradedAt: "2026-09-20T09:00:00" }),
        ]),
      ),
    );
    renderPage();

    const first = (await screen.findByRole("heading", { name: "Term exam" })).closest("li")!;
    expect(within(first).getByText("14.5")).toBeInTheDocument();
    expect(within(first).getByText("Class 7A")).toBeInTheDocument();
    expect(within(first).getByText("Theo Teacher")).toBeInTheDocument();
    expect(first.querySelector('time[datetime="2026-10-01T09:00:00"]')).toBeInTheDocument();
    const second = screen.getByRole("heading", { name: "Quiz" }).closest("li")!;
    expect(within(second).getByText("Class 7B")).toBeInTheDocument();
    expect(within(second).getByText("Tina Teacher")).toBeInTheDocument();
    expect(screen.getByText("Showing 1–2 of 2 results")).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent(/%|Geography|English|Achievements|Performance|James Green|Stella/);

    expect(requests).toHaveLength(1);
    expect(requests[0].get("page")).toBe("0");
    expect(requests[0].get("size")).toBe("10");
    expect(requests[0].get("sort")).toBe("gradedAt,desc");
  });

  it("sends the Authorization header of the signed-in student", async () => {
    let authorization: string | null = null;
    server.use(
      http.get(apiUrl(`/v1/grades/student/${alice.id}/paged`), ({ request }) => {
        authorization = request.headers.get("Authorization");
        return HttpResponse.json(page([result(1)]));
      }),
    );
    renderPage();

    await screen.findByRole("heading", { name: "Assessment 1" });
    expect(authorization).toBe("Bearer alice-token");
  });

  it("keeps a real zero score and says when a score is not recorded", async () => {
    serveGrades(alice.id, () =>
      HttpResponse.json(page([result(1, { content: "Zero test", score: 0 }), result(2, { content: "Pending test", score: null })])),
    );
    renderPage();

    const zero = (await screen.findByRole("heading", { name: "Zero test" })).closest("li")!;
    expect(within(zero).getByText("0")).toBeInTheDocument();
    expect(within(zero).queryByText("Not recorded")).not.toBeInTheDocument();
    const pending = screen.getByRole("heading", { name: "Pending test" }).closest("li")!;
    expect(within(pending).getByText("Not recorded")).toBeInTheDocument();
  });

  it("omits optional fields that are missing instead of inventing them", async () => {
    serveGrades(alice.id, () =>
      HttpResponse.json(
        page([result(1, { content: null, className: null, teacherName: null, gradedAt: null, score: 3, courseName: null })]),
      ),
    );
    renderPage();

    const item = (await screen.findByRole("heading", { name: "Untitled result" })).closest("li")!;
    expect(within(item).getByText("3")).toBeInTheDocument();
    expect(within(item).queryByText(/Class:|Teacher:|Course:|Graded on:/)).not.toBeInTheDocument();
    expect(item.querySelector("time")).toBeNull();
  });

  it("shows the course only when the backend returns one", async () => {
    serveGrades(alice.id, () => HttpResponse.json(page([result(1, { courseName: "Algebra" })])));
    renderPage();

    expect(await screen.findByText("Algebra")).toBeInTheDocument();
  });

  it("shows an honest empty state, not an error", async () => {
    serveGrades(alice.id, () => HttpResponse.json(page([])));
    renderPage();

    expect(await screen.findByText("No results have been recorded for you yet.")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Results pages" })).not.toBeInTheDocument();
  });

  it("shows a loading state before the response arrives", async () => {
    serveGrades(alice.id, async () => {
      await delay(50);
      return HttpResponse.json(page([result(1)]));
    });
    renderPage();

    expect(screen.getByRole("status")).toHaveTextContent("Loading your results…");
    expect(await screen.findByRole("heading", { name: "Assessment 1" })).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });
});

describe("student results pagination", () => {
  const pages: Record<string, StudentResult[]> = {
    "0": Array.from({ length: 10 }, (_, i) => result(i + 1)),
    "1": Array.from({ length: 10 }, (_, i) => result(i + 11)),
    "2": [result(21, { content: "Last assessment" })],
  };

  function servePages() {
    return serveGrades(alice.id, (params) => {
      const index = params.get("page") ?? "0";
      return HttpResponse.json(page(pages[index] ?? [], Number(index), 21));
    });
  }

  it("moves between backend pages with the totals reported by the backend", async () => {
    const user = userEvent.setup();
    const requests = servePages();
    renderPage();

    expect(await screen.findByText("Showing 1–10 of 21 results")).toBeInTheDocument();
    expect(screen.getByText("Page 1 of 3")).toBeInTheDocument();
    const previous = screen.getByRole("button", { name: "Go to previous page" });
    expect(previous).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Go to next page" }));
    expect(await screen.findByText("Showing 11–20 of 21 results")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Assessment 11" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Assessment 1" })).not.toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent("/student/results?page=2");

    await user.click(screen.getByRole("button", { name: "Go to next page" }));
    expect(await screen.findByText("Showing 21–21 of 21 results")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Last assessment" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Go to next page" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Go to previous page" }));
    expect(await screen.findByText("Showing 11–20 of 21 results")).toBeInTheDocument();
    expect(requests.map((params) => params.get("page"))).toEqual(["0", "1", "2", "1"]);
  });

  it("opens the page named in the URL", async () => {
    const requests = servePages();
    renderPage("/student/results?page=3");

    expect(await screen.findByRole("heading", { name: "Last assessment" })).toBeInTheDocument();
    expect(requests.map((params) => params.get("page"))).toEqual(["2"]);
  });

  it("treats an invalid page parameter as the first page", async () => {
    const requests = servePages();
    renderPage("/student/results?page=-4");

    expect(await screen.findByText("Showing 1–10 of 21 results")).toBeInTheDocument();
    expect(requests.map((params) => params.get("page"))).toEqual(["0"]);
  });

  it("moves to the last page when the requested page is past the end", async () => {
    const requests = servePages();
    renderPage("/student/results?page=9");

    expect(await screen.findByRole("heading", { name: "Last assessment" })).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent("/student/results?page=3");
    expect(requests.map((params) => params.get("page"))).toEqual(["8", "2"]);
  });
});

describe("student results failures", () => {
  it.each([403, 404, 500])("shows an error, not an empty list, for HTTP %i", async (status) => {
    serveGrades(alice.id, () => HttpResponse.json({ title: "Failure" }, { status }));
    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to load your results.");
    expect(screen.queryByText("No results have been recorded for you yet.")).not.toBeInTheDocument();
  });

  it("ends the session, without showing an empty list, when the token is rejected and cannot be renewed", async () => {
    serveGrades(alice.id, () => HttpResponse.json({ title: "Unauthorized" }, { status: 401 }));
    server.use(http.post(apiUrl("/auth/refresh"), () => HttpResponse.json({ title: "Unauthorized" }, { status: 401 })));
    renderPage();

    await waitFor(() => expect(store.getState().auth.user).toBeNull());
    expect(screen.queryByText("No results have been recorded for you yet.")).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { level: 1 })).not.toBeInTheDocument();
  });

  it("shows an error for a network failure", async () => {
    serveGrades(alice.id, () => HttpResponse.error());
    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to load your results.");
    expect(screen.queryByText("No results have been recorded for you yet.")).not.toBeInTheDocument();
  });

  it("retries a failed request and shows the results", async () => {
    const user = userEvent.setup();
    let calls = 0;
    serveGrades(alice.id, () =>
      ++calls === 1 ? HttpResponse.json({ title: "Failure" }, { status: 500 }) : HttpResponse.json(page([result(1)])),
    );
    renderPage();

    await user.click(await screen.findByRole("button", { name: "Retry" }));
    expect(await screen.findByRole("heading", { name: "Assessment 1" })).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(calls).toBe(2);
  });

  it("rejects a response that contains another student's result", async () => {
    serveGrades(alice.id, () => HttpResponse.json(page([result(1), result(2, { studentId: bob.id, content: "Bob's secret" })])));
    renderPage();

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(screen.queryByText("Bob's secret")).not.toBeInTheDocument();
  });
});

describe("student results identity", () => {
  it("requests only the signed-in student's results, never an ID from the URL", async () => {
    const requests = serveGrades(alice.id, () => HttpResponse.json(page([result(1)])));
    // No handler for student 22: such a request would fail the test as unhandled.
    renderPage("/student/results?studentId=22&id=22");

    await screen.findByRole("heading", { name: "Assessment 1" });
    expect(requests).toHaveLength(1);
  });

  it("does not request or render anything for a signed-in user who is not a student", async () => {
    act(() => logIn({ ...alice, role: "TEACHER" }, "teacher-token"));
    renderPage();

    await waitFor(() => expect(screen.queryByRole("heading", { level: 1 })).not.toBeInTheDocument());
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("drops the previous student's results when another student signs in", async () => {
    serveGrades(alice.id, () => HttpResponse.json(page([result(1, { content: "Alice's exam" })])));
    serveGrades(bob.id, () =>
      HttpResponse.json(page([result(5, { content: "Bob's exam", studentId: bob.id })])),
    );
    renderPage();
    expect(await screen.findByRole("heading", { name: "Alice's exam" })).toBeInTheDocument();

    act(() => terminateSession());
    expect(screen.queryByText("Alice's exam")).not.toBeInTheDocument();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);

    act(() => logIn(bob, "bob-token"));
    expect(await screen.findByRole("heading", { name: "Bob's exam" })).toBeInTheDocument();
    expect(screen.queryByText("Alice's exam")).not.toBeInTheDocument();
  });

  it("keeps the previous student's cached results from showing while another student loads, without a logout", async () => {
    serveGrades(alice.id, () => HttpResponse.json(page([result(1, { content: "Alice's exam" })])));
    serveGrades(bob.id, async () => {
      await delay(50);
      return HttpResponse.json(page([result(5, { content: "Bob's exam", studentId: bob.id })]));
    });
    renderPage();
    expect(await screen.findByRole("heading", { name: "Alice's exam" })).toBeInTheDocument();

    act(() => logIn(bob, "bob-token"));
    await waitFor(() => expect(screen.queryByText("Alice's exam")).not.toBeInTheDocument());
    expect(await screen.findByRole("heading", { name: "Bob's exam" })).toBeInTheDocument();
  });
});
