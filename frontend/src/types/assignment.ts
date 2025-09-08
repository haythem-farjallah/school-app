// Assignment Management System Types
// Comprehensive TypeScript interfaces for assignment functionality

export type AssignmentType = 'homework' | 'quiz' | 'exam' | 'project' | 'lab' | 'presentation' | 'essay' | 'other';

export type AssignmentStatus = 'draft' | 'published' | 'active' | 'closed' | 'archived';

export type SubmissionStatus = 'not_submitted' | 'submitted' | 'late' | 'graded' | 'returned';

export type GradingStatus = 'not_graded' | 'in_progress' | 'graded' | 'reviewed';

export type Priority = 'low' | 'medium' | 'high' | 'urgent';

// Core Assignment Interface
export interface Assignment {
  id: string;
  title: string;
  description: string;
  type: AssignmentType;
  status: AssignmentStatus;
  priority: Priority;
  
  // Academic Information
  courseId: string;
  courseName: string;
  classId: string;
  className: string;
  teacherId: string;
  teacherName: string;
  
  // Scheduling
  createdAt: Date;
  updatedAt: Date;
  publishedAt?: Date;
  dueDate: Date;
  availableFrom?: Date;
  availableUntil?: Date;
  
  // Grading Configuration
  totalPoints: number;
  passingScore?: number;
  gradingRubric?: GradingRubric;
  allowLateSubmissions: boolean;
  latePenalty?: number; // percentage deduction per day
  maxLateDays?: number;
  
  // Submission Settings
  submissionTypes: SubmissionType[];
  maxFileSize?: number; // in MB
  allowedFileTypes?: string[];
  requiresTextSubmission: boolean;
  requiresFileSubmission: boolean;
  maxSubmissions?: number;
  
  // Visibility & Access
  isVisible: boolean;
  visibleToStudents: boolean;
  instructions?: string;
  resources?: AssignmentResource[];
  
  // Statistics
  totalStudents: number;
  submissionCount: number;
  gradedCount: number;
  averageScore?: number;
  
  // Metadata
  tags?: string[];
  category?: string;
  estimatedDuration?: number; // in minutes
}

// Assignment Creation/Update DTOs
export interface CreateAssignmentRequest {
  title: string;
  description: string;
  type: AssignmentType;
  courseId: string;
  classId: string;
  dueDate: Date;
  totalPoints: number;
  instructions?: string;
  submissionTypes: SubmissionType[];
  allowLateSubmissions: boolean;
  latePenalty?: number;
  maxLateDays?: number;
  requiresTextSubmission: boolean;
  requiresFileSubmission: boolean;
  allowedFileTypes?: string[];
  maxFileSize?: number;
  availableFrom?: Date;
  availableUntil?: Date;
  priority: Priority;
  tags?: string[];
  resources?: Omit<AssignmentResource, 'id' | 'createdAt'>[];
}

export interface UpdateAssignmentRequest extends Partial<CreateAssignmentRequest> {
  id: string;
  status?: AssignmentStatus;
}

// Submission Types
export type SubmissionType = 'text' | 'file' | 'url' | 'media';

// Assignment Resources (attachments, links, etc.)
export interface AssignmentResource {
  id: string;
  name: string;
  type: 'file' | 'link' | 'video' | 'document';
  url: string;
  size?: number;
  mimeType?: string;
  description?: string;
  createdAt: Date;
}

// Grading Rubric System
export interface GradingRubric {
  id: string;
  name: string;
  description?: string;
  criteria: RubricCriterion[];
  totalPoints: number;
}

export interface RubricCriterion {
  id: string;
  name: string;
  description: string;
  maxPoints: number;
  levels: RubricLevel[];
}

export interface RubricLevel {
  id: string;
  name: string;
  description: string;
  points: number;
}

// Student Submission System
export interface Submission {
  id: string;
  assignmentId: string;
  studentId: string;
  studentName: string;
  studentEmail: string;
  
  // Submission Content
  textContent?: string;
  files: SubmissionFile[];
  urls?: string[];
  
  // Status & Timing
  status: SubmissionStatus;
  submittedAt: Date;
  isLate: boolean;
  daysLate?: number;
  attemptNumber: number;
  
  // Grading
  grade?: Grade;
  feedback?: Feedback;
  gradingStatus: GradingStatus;
  gradedAt?: Date;
  gradedBy?: string;
  
  // Metadata
  ipAddress?: string;
  userAgent?: string;
  lastModified: Date;
}

export interface SubmissionFile {
  id: string;
  originalName: string;
  fileName: string;
  size: number;
  mimeType: string;
  url: string;
  uploadedAt: Date;
}

// Grading System
export interface Grade {
  id: string;
  submissionId: string;
  assignmentId: string;
  studentId: string;
  
  // Score Information
  score: number;
  maxScore: number;
  percentage: number;
  letterGrade?: string;
  
  // Rubric Grading
  rubricScores?: RubricScore[];
  
  // Metadata
  gradedBy: string;
  gradedAt: Date;
  isExcused: boolean;
  notes?: string;
}

export interface RubricScore {
  criterionId: string;
  levelId: string;
  points: number;
  feedback?: string;
}

// Feedback System
export interface Feedback {
  id: string;
  submissionId: string;
  teacherId: string;
  teacherName: string;
  
  // Feedback Content
  generalComments?: string;
  privateComments?: string;
  audioFeedback?: string; // URL to audio file
  videoFeedback?: string; // URL to video file
  
  // Inline Comments (for text submissions)
  inlineComments?: InlineComment[];
  
  // File Annotations (for file submissions)
  fileAnnotations?: FileAnnotation[];
  
  // Metadata
  createdAt: Date;
  updatedAt: Date;
  isVisible: boolean;
}

export interface InlineComment {
  id: string;
  startPosition: number;
  endPosition: number;
  comment: string;
  createdAt: Date;
}

export interface FileAnnotation {
  id: string;
  fileId: string;
  pageNumber?: number;
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  comment: string;
  type: 'highlight' | 'note' | 'correction';
  createdAt: Date;
}

// Assignment Analytics & Statistics
export interface AssignmentAnalytics {
  assignmentId: string;
  
  // Submission Statistics
  totalStudents: number;
  submittedCount: number;
  notSubmittedCount: number;
  lateSubmissionCount: number;
  submissionRate: number;
  
  // Grading Statistics
  gradedCount: number;
  averageScore: number;
  medianScore: number;
  highestScore: number;
  lowestScore: number;
  standardDeviation: number;
  
  // Grade Distribution
  gradeDistribution: GradeDistribution[];
  
  // Time Analytics
  averageSubmissionTime: number; // minutes before deadline
  peakSubmissionTimes: Date[];
  
  // Performance Insights
  strugglingStudents: StudentPerformance[];
  topPerformers: StudentPerformance[];
}

export interface GradeDistribution {
  range: string; // e.g., "90-100", "80-89"
  count: number;
  percentage: number;
}

export interface StudentPerformance {
  studentId: string;
  studentName: string;
  score: number;
  percentage: number;
  submissionStatus: SubmissionStatus;
  isLate: boolean;
}

// Assignment Templates (for reusability)
export interface AssignmentTemplate {
  id: string;
  name: string;
  description: string;
  type: AssignmentType;
  category: string;
  
  // Template Configuration
  defaultDuration: number; // days
  defaultPoints: number;
  defaultInstructions: string;
  defaultSubmissionTypes: SubmissionType[];
  defaultRubric?: GradingRubric;
  
  // Usage Statistics
  usageCount: number;
  createdBy: string;
  createdAt: Date;
  lastUsed?: Date;
  
  // Template Content
  resources?: Omit<AssignmentResource, 'id' | 'createdAt'>[];
  tags?: string[];
}

// Bulk Operations
export interface BulkAssignmentOperation {
  operation: 'publish' | 'close' | 'extend_deadline' | 'delete' | 'archive';
  assignmentIds: string[];
  parameters?: {
    newDueDate?: Date;
    extensionDays?: number;
    reason?: string;
  };
}

export interface BulkGradingOperation {
  operation: 'grade' | 'return' | 'excuse' | 'reset';
  submissionIds: string[];
  parameters?: {
    score?: number;
    feedback?: string;
    reason?: string;
  };
}

// Search and Filter Types
export interface AssignmentFilters {
  status?: AssignmentStatus[];
  type?: AssignmentType[];
  courseId?: string;
  classId?: string;
  teacherId?: string;
  priority?: Priority[];
  dueDateFrom?: Date;
  dueDateTo?: Date;
  createdFrom?: Date;
  createdTo?: Date;
  tags?: string[];
  hasSubmissions?: boolean;
  needsGrading?: boolean;
  isOverdue?: boolean;
}

export interface SubmissionFilters {
  status?: SubmissionStatus[];
  gradingStatus?: GradingStatus[];
  isLate?: boolean;
  assignmentId?: string;
  studentId?: string;
  submittedFrom?: Date;
  submittedTo?: Date;
  scoreMin?: number;
  scoreMax?: number;
}

// API Response Types
export interface AssignmentListResponse {
  assignments: Assignment[];
  totalCount: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface SubmissionListResponse {
  submissions: Submission[];
  totalCount: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

// Form Validation Schemas (for use with react-hook-form)
export interface AssignmentFormData {
  title: string;
  description: string;
  type: AssignmentType;
  courseId: string;
  classId: string;
  dueDate: string; // ISO string for form handling
  totalPoints: number;
  instructions: string;
  submissionTypes: SubmissionType[];
  allowLateSubmissions: boolean;
  latePenalty: number;
  maxLateDays: number;
  requiresTextSubmission: boolean;
  requiresFileSubmission: boolean;
  allowedFileTypes: string[];
  maxFileSize: number;
  availableFrom: string;
  availableUntil: string;
  priority: Priority;
  tags: string;
}

// Notification Types
export interface AssignmentNotification {
  id: string;
  type: 'assignment_created' | 'assignment_updated' | 'due_soon' | 'overdue' | 'graded' | 'feedback_available';
  assignmentId: string;
  assignmentTitle: string;
  recipientId: string;
  recipientType: 'student' | 'teacher' | 'parent';
  message: string;
  data?: Record<string, string | number | boolean>;
  isRead: boolean;
  createdAt: Date;
}

// All types are already exported above with their declarations
