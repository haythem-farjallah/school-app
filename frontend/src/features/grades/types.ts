/** One recorded grade, as returned by GET /v1/grades/student/{studentId}/paged. */
export interface StudentResult {
  id: number;
  content: string | null;
  score: number | null;
  gradedAt: string | null;
  studentId: number;
  classId: number | null;
  className: string | null;
  teacherId: number | null;
  teacherName: string | null;
  courseName?: string | null;
}
