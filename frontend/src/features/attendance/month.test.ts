import { describe, expect, it } from "vitest";
import { adjacentAttendanceMonth, attendanceMonth } from "./month";

describe("attendance calendar selection", () => {
  it("uses the local month at the first minutes of January", () => {
    expect(attendanceMonth(new URLSearchParams(), new Date(2026, 0, 1, 0, 15))).toBe("2026-01");
  });

  it("uses the local month at the last minutes of December", () => {
    expect(attendanceMonth(new URLSearchParams(), new Date(2025, 11, 31, 23, 45))).toBe("2025-12");
  });

  it("keeps a bookmarked month independent of today's date", () => {
    expect(attendanceMonth(new URLSearchParams("month=2024-02"), new Date(2026, 0, 1))).toBe("2024-02");
  });

  it("keeps four-digit years when navigating January and December", () => {
    expect(adjacentAttendanceMonth("0099-12", 1)).toBe("0100-01");
    expect(adjacentAttendanceMonth("0100-01", -1)).toBe("0099-12");
  });
});
