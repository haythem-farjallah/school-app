import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { HelmetProvider } from "react-helmet-async";
import Grades from "./Grades";
import { Semester, type StudentGradeSheet } from "@/types/grade";

const { readSheet } = vi.hoisted(() => ({ readSheet: vi.fn() }));
vi.mock("@/lib/auth", () => ({ useAuth: () => ({ user: { id: 5 } }) }));
vi.mock("@/features/grades/hooks/use-grades", () => ({
  useStudentGradeSheet: readSheet,
  useExportGradeSheet: () => ({ isPending: false }),
}));

const sheet: StudentGradeSheet = {
  studentId: 5, studentFirstName: "Sam", studentLastName: "Student", studentEmail: "sam@example.test",
  classId: 4, className: "7A", yearOfStudy: 7, semester: Semester.FIRST, subjects: [],
  totalScore: 15, totalMaxScore: 20, weightedAverage: 75, totalStudents: 1,
  classRank: null, attendanceRate: null, totalAbsences: null, generatedAt: "2026-10-01T10:00:00",
  approvedBy: { staffId: null, staffName: "Principal", approvedAt: "2026-10-01T09:00:00" },
};

describe("student grade metric honesty", () => {
  it("renders unavailable metrics without inventing rank, percentage or absence count", () => {
    readSheet.mockReturnValue({ data: sheet, isLoading: false });
    render(<HelmetProvider><Grades /></HelmetProvider>);
    expect(screen.getAllByText("N/A")).toHaveLength(6);
    expect(screen.queryByText("#0")).not.toBeInTheDocument();
    expect(screen.queryByText("0.0%")).not.toBeInTheDocument();
    expect(screen.queryByText("0 absences")).not.toBeInTheDocument();
    expect(screen.getByText("By: Principal")).toBeInTheDocument();
  });

  it("preserves real zero values", () => {
    readSheet.mockReturnValue({ data: { ...sheet, classRank: 1, attendanceRate: 0, totalAbsences: 0 }, isLoading: false });
    render(<HelmetProvider><Grades /></HelmetProvider>);
    expect(screen.getAllByText("0.0%")).toHaveLength(2);
    expect(screen.getByText("0 absences")).toBeInTheDocument();
    expect(screen.getByText("#1")).toBeInTheDocument();
  });
});
