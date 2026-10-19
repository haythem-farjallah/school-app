/** Fields returned by DashboardServiceImpl, rather than the unused StudentDashboardDto. */
export interface StudentDashboardData {
  type: "STUDENT";
  baseInfo: {
    userInfo: { id: number; name: string; email: string; role: string };
    notifications: {
      id: number;
      title: string;
      message: string;
      type: string;
      isRead: boolean;
      createdAt: string | null;
    }[];
  };
  stats: { totalEnrollments: number; averageGrade: number | null };
  enrolledClasses: {
    classId: number;
    className: string;
    teacherName: string;
    totalStudents: number;
  }[];
  recentGrades: {
    // The backend currently populates courseName with the enrollment's class name.
    courseName: string | null;
    score: number | null;
    content: string | null;
    gradedAt: string | null;
  }[];
}
