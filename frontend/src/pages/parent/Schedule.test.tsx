import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Schedule from "./Schedule";

describe("parent Schedule", () => {
  // Rendered without a QueryClientProvider: any server query would throw.
  it("says the schedule is not available and shows no timetable", () => {
    render(<Schedule />);

    expect(screen.getByText("Children's schedules are not available yet.")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
