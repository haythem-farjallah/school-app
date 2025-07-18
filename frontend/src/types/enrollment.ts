export enum EnrollmentStatus {
  PENDING = "PENDING",
  ACTIVE = "ACTIVE",
  COMPLETED = "COMPLETED",
  DROPPED = "DROPPED",
  SUSPENDED = "SUSPENDED",
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
  status: EnrollmentStatus;
}

export interface TransferStudentRequest {
  newClassId: number;
}

export interface DropEnrollmentRequest {
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
  pendingEnrollments: number;
  completedEnrollments: number;
  droppedEnrollments: number;
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
    case EnrollmentStatus.PENDING:
      return "bg-yellow-100 text-yellow-800 border-yellow-200";
    case EnrollmentStatus.ACTIVE:
      return "bg-green-100 text-green-800 border-green-200";
    case EnrollmentStatus.COMPLETED:
      return "bg-blue-100 text-blue-800 border-blue-200";
    case EnrollmentStatus.DROPPED:
      return "bg-red-100 text-red-800 border-red-200";
    case EnrollmentStatus.SUSPENDED:
      return "bg-gray-100 text-gray-800 border-gray-200";
    default:
      return "bg-gray-100 text-gray-800 border-gray-200";
  }
}

// Helper function to get status label
export function getEnrollmentStatusLabel(status: EnrollmentStatus): string {
  switch (status) {
    case EnrollmentStatus.PENDING:
      return "Pending";
    case EnrollmentStatus.ACTIVE:
      return "Active";
    case EnrollmentStatus.COMPLETED:
      return "Completed";
    case EnrollmentStatus.DROPPED:
      return "Dropped";
    case EnrollmentStatus.SUSPENDED:
      return "Suspended";
    default:
      return status;
  }
} 