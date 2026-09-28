import { describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { TeacherAttendanceMarking } from "./teacher-attendance-marking";
import { apiUrl, server } from "@/test/server";

const MONDAY = "2030-01-07";

const slot = (id: number, dayOfWeek: string, className: string) => ({
  id,
  dayOfWeek,
  period: { id: 1, index: 2, startTime: "09:00", endTime: "10:00" },
  forClass: { id: 4, name: className },
  forCourse: { id: 6, name: "Maths" },
});

function renderMarking() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <TeacherAttendanceMarking teacherId={9} selectedDate={MONDAY} />
    </QueryClientProvider>,
  );
}

describe("TeacherAttendanceMarking", () => {
  it("marks attendance through the teacher's own slot on the selected day", async () => {
    let marked: unknown = null;
    server.use(
      http.get(apiUrl("/v1/timetables/teacher/9"), () =>
        HttpResponse.json({ status: "success", data: [slot(31, "MONDAY", "7A"), slot(32, "TUESDAY", "8B")] }),
      ),
      http.get(apiUrl("/v1/attendance/teacher/9/absent-students"), () => HttpResponse.json({ status: "success", data: [] })),
      http.get(apiUrl("/v1/attendance/slot/31/students"), () =>
        HttpResponse.json({
          status: "success",
          data: [{ userId: 11, userName: "Sam Student", status: "PRESENT", timetableSlotId: 31, date: MONDAY, userType: "STUDENT" }],
        }),
      ),
      http.get(apiUrl("/v1/attendance/teacher/9/can-mark/31"), () => HttpResponse.json({ status: "success", data: true })),
      http.post(apiUrl("/v1/attendance/slot/31/mark"), async ({ request }) => {
        marked = await request.json();
        return HttpResponse.json({ status: "success", data: [] }, { status: 201 });
      }),
    );
    renderMarking();
    const user = userEvent.setup();

    await user.click(await screen.findByText("7A"));
    expect(screen.queryByText("8B")).not.toBeInTheDocument();

    await user.click(screen.getByRole("tab", { name: "Mark Attendance" }));
    expect(await screen.findByText("Sam Student")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Save Attendance" }));

    await waitFor(() =>
      expect(marked).toEqual([
        expect.objectContaining({ userId: 11, timetableSlotId: 31, date: MONDAY, status: "PRESENT" }),
      ]),
    );
  });

  it("says so when the teacher has no slot on the selected day", async () => {
    server.use(
      http.get(apiUrl("/v1/timetables/teacher/9"), () =>
        HttpResponse.json({ status: "success", data: [slot(32, "TUESDAY", "8B")] }),
      ),
      http.get(apiUrl("/v1/attendance/teacher/9/absent-students"), () => HttpResponse.json({ status: "success", data: [] })),
    );
    renderMarking();

    expect(await screen.findByText("No timetable slots on this day")).toBeInTheDocument();
  });
});
