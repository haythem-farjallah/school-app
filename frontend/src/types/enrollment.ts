/** ACTIVE is the only live status; the other three are terminal history. */
export enum EnrollmentStatus {
  ACTIVE = "ACTIVE",
  COMPLETED = "COMPLETED",
  TRANSFERRED = "TRANSFERRED",
  WITHDRAWN = "WITHDRAWN",
}

/** Statuses an administrator may set by hand. TRANSFERRED only results from a transfer. */
export const MANUAL_ENROLLMENT_STATUSES = [EnrollmentStatus.COMPLETED, EnrollmentStatus.WITHDRAWN] as const;

export function isTerminalEnrollmentStatus(status: EnrollmentStatus): boolean {
  return status !== EnrollmentStatus.ACTIVE;
}

export interface Enrollment {
  id: number;
  studentName: string;
  studentEmail: string;
  className: string;
  gradeCount: number;
  enrolledAt: string; // LocalDate as string
  status: EnrollmentStatus;
  finalGrad?: number;
  studentId: number;
  classId: number;
}

export interface CreateEnrollmentRequest {
  studentId: number;
  classId: number;
}

export interface UpdateEnrollmentStatusRequest {
  status: (typeof MANUAL_ENROLLMENT_STATUSES)[number];
}

export interface TransferStudentRequest {
  newClassId: number;
}

export interface WithdrawEnrollmentRequest {
  reason: string;
}

export interface BulkEnrollStudentsRequest {
  classId: number;
  studentIds: number[];
}

export interface EnrollmentFilters {
  search?: string;
  status?: EnrollmentStatus;
  studentId?: number;
  classId?: number;
  startDate?: string;
  endDate?: string;
}

export interface EnrollmentStats {
  totalEnrollments: number;
  activeEnrollments: number;
  completedEnrollments: number;
  transferredEnrollments: number;
  withdrawnEnrollments: number;
  completionRate: number;
  averageFinalGrade: number;
}

export interface EnrollmentResponse {
  data: Enrollment[];
  total: number;
  page: number;
  size: number;
}

// Helper function to get status color
export function getEnrollmentStatusColor(status: EnrollmentStatus): string {
  switch (status) {
    case EnrollmentStatus.ACTIVE:
      return "bg-green-100 text-green-800 border-green-200";
    case EnrollmentStatus.COMPLETED:
      return "bg-blue-100 text-blue-800 border-blue-200";
    case EnrollmentStatus.TRANSFERRED:
      return "bg-purple-100 text-purple-800 border-purple-200";
    case EnrollmentStatus.WITHDRAWN:
      return "bg-red-100 text-red-800 border-red-200";
    default:
      return "bg-gray-100 text-gray-800 border-gray-200";
  }
}

// Helper function to get status label
export function getEnrollmentStatusLabel(status: EnrollmentStatus): string {
  switch (status) {
    case EnrollmentStatus.ACTIVE:
      return "Active";
    case EnrollmentStatus.COMPLETED:
      return "Completed";
    case EnrollmentStatus.TRANSFERRED:
      return "Transferred";
    case EnrollmentStatus.WITHDRAWN:
      return "Withdrawn";
    default:
      return status;
  }
} 