import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Grades from "./Grades";

describe("admin and staff Grades", () => {
  // Rendered without a QueryClientProvider: any server query would throw.
  it("says grade management is not available and offers no grade list or grade actions", () => {
    render(<Grades />);

    expect(screen.getByText("Grade management is not available yet.")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
