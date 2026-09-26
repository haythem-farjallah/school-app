import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Assignments from "./Assignments";

describe("teacher Assignments", () => {
  // Rendered without a QueryClientProvider: any server query would throw.
  it("says assignments are not available and offers no assignment actions", () => {
    render(<Assignments />);

    expect(screen.getByText("Assignments are not available yet.")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
