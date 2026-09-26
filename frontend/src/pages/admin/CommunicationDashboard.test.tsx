import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import CommunicationDashboard from "./CommunicationDashboard";

describe("admin CommunicationDashboard", () => {
  // Rendered without a QueryClientProvider: any server query would throw.
  it("says communication analytics are not available and shows no figures", () => {
    render(<CommunicationDashboard />);

    expect(screen.getByText("Communication analytics are not available yet.")).toBeInTheDocument();
    expect(screen.queryByText(/\d/)).not.toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
