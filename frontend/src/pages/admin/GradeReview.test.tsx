import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { HelmetProvider } from "react-helmet-async";
import GradeReview from "./GradeReview";
import { Semester, type StaffGradeReview } from "@/types/grade";

const { readReviews } = vi.hoisted(() => ({ readReviews: vi.fn() }));
vi.mock("@/features/classes/hooks/use-classes", () => ({ useClasses: () => ({ data: { data: [] } }) }));
vi.mock("@/features/grades/hooks/use-grades", () => ({
  useStaffGradeReviews: readReviews,
  useApproveGrades: () => ({ isPending: false }),
  useExportGradeSheet: () => ({ isPending: false }),
}));
const review: StaffGradeReview = {
  studentId: 5, studentFirstName: "Sam", studentLastName: "Student", classId: 4, className: "7A",
  semester: Semester.FIRST, subjects: [], overallAverage: 75,
  classRank: null, attendanceRate: null, isApproved: false,
};

describe("staff grade metric honesty", () => {
  it("renders null metrics in the table and student details", () => {
    readReviews.mockReturnValue({ data: [review], isLoading: false });
    render(<HelmetProvider><GradeReview /></HelmetProvider>);
    const row = screen.getByText("Sam Student").closest("tr")!;
    expect(within(row).getAllByText("N/A")).toHaveLength(2);
    fireEvent.click(within(row).getAllByRole("button")[0]);
    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByText("Class Rank: N/A")).toBeInTheDocument();
    expect(within(dialog).getByText("Attendance: N/A")).toBeInTheDocument();
  });

  it("preserves a real zero attendance rate", () => {
    readReviews.mockReturnValue({ data: [{ ...review, classRank: 1, attendanceRate: 0 }], isLoading: false });
    render(<HelmetProvider><GradeReview /></HelmetProvider>);
    expect(screen.getByText("0.0%")).toBeInTheDocument();
    expect(screen.getByText("#1")).toBeInTheDocument();
  });
});
