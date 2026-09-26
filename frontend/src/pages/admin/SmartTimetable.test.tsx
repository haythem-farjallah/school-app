import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import SmartTimetable from "./SmartTimetable";

describe("admin SmartTimetable", () => {
  // Rendered without a QueryClientProvider: any server query would throw.
  it("says optimization is not available and offers no optimizer actions", () => {
    render(<SmartTimetable />);

    expect(screen.getByText("Timetable optimization is not available yet.")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
