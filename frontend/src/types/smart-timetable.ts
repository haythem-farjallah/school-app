// Smart Timetable System Types
export interface TimetableOptimizationRequest {
  timetableId: number;
  optimizationTimeSeconds?: number;
  
  // Priority Weights (0.0 to 1.0)
  teacherWorkloadWeight?: number;
  roomOptimizationWeight?: number;
  studentConvenienceWeight?: number;
  resourceEfficiencyWeight?: number;
  
  // Constraints
  hardConstraints?: string[];
  softConstraints?: string[];
  customConstraints?: Record<string, any>;
  
  // Teacher Preferences
  teacherPreferences?: Record<number, string[]>; // teacherId -> preferred time slots
  teacherAvoidances?: Record<number, string[]>;  // teacherId -> avoided time slots
  
  // Class Preferences
  maxDailyHoursPerClass?: Record<number, number>;   // classId -> max hours per day
  preferredSubjectTimes?: Record<number, string[]>; // courseId -> preferred times
  
  // Room Constraints
  roomRestrictions?: Record<number, number[]>;     // roomId -> allowed course types
  roomCapacityOverrides?: Record<number, number>;   // roomId -> capacity override
  
  // Advanced Options
  enableAIOptimization?: boolean;
  enableWorkloadBalancing?: boolean;
  enableConflictResolution?: boolean;
  enableRoomOptimization?: boolean;
  
  // Scenario Generation
  generateMultipleScenarios?: boolean;
  scenarioCount?: number;
  
  // Quality Thresholds
  minimumAcceptableScore?: number;
  maxIterationsWithoutImprovement?: number;
}

export interface TimetableOptimizationResult {
  timetableId: number;
  optimizationId: string;
  status: OptimizationStatus;
  startTime: string;
  endTime: string;
  durationMs: number;
  
  // Scoring
  finalScore: number;
  hardConstraintScore: number;
  softConstraintScore: number;
  improvementPercentage: number;
  
  // Quality Metrics
  qualityMetrics: QualityMetrics;
  
  // Conflicts & Issues
  resolvedConflicts: ConflictInfo[];
  remainingConflicts: ConflictInfo[];
  warnings: string[];
  
  // Teacher Analysis
  teacherWorkloads: TeacherWorkloadSummary[];
  averageTeacherWorkload: number;
  workloadStandardDeviation: number;
  
  // Room Analysis
  roomUtilizations: RoomUtilizationSummary[];
  averageRoomUtilization: number;
  
  // Student Analysis
  studentAnalysis: Record<number, StudentScheduleAnalysis>; // classId -> analysis
  
  // Alternative Scenarios (if requested)
  alternativeScenarios: TimetableScenario[];
  
  // Recommendations
  recommendations: OptimizationRecommendation[];
}

export type OptimizationStatus = 
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'PARTIALLY_COMPLETED';

export interface QualityMetrics {
  teacherSatisfaction: number;
  studentSatisfaction: number;
  resourceEfficiency: number;
  scheduleCompactness: number;
  conflictResolutionRate: number;
  totalViolations: number;
  hardViolations: number;
  softViolations: number;
}

export interface ConflictInfo {
  conflictType: string;
  description: string;
  affectedSlotIds: number[];
  affectedTeacherIds: number[];
  affectedClassIds: number[];
  affectedRoomIds: number[];
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  resolution: string;
  resolved: boolean;
}

export interface TeacherWorkloadSummary {
  teacherId: number;
  teacherName: string;
  totalHours: number;
  maxCapacity: number;
  utilizationRate: number;
  gapHours: number;
  consecutiveHours: number;
  workloadIssues: string[];
  workloadRating: 'OPTIMAL' | 'OVERLOADED' | 'UNDERUTILIZED';
}

export interface RoomUtilizationSummary {
  roomId: number;
  roomName: string;
  capacity: number;
  totalHoursUsed: number;
  maxPossibleHours: number;
  utilizationRate: number;
  utilizationIssues: string[];
  utilizationRating: 'OPTIMAL' | 'OVERUSED' | 'UNDERUSED';
}

export interface StudentScheduleAnalysis {
  classId: number;
  className: string;
  totalHours: number;
  maxConsecutiveHours: number;
  totalGaps: number;
  scheduleCompactness: number;
  scheduleIssues: string[];
  scheduleRating: 'EXCELLENT' | 'GOOD' | 'NEEDS_IMPROVEMENT';
}

export interface TimetableScenario {
  scenarioId: string;
  scenarioName: string;
  score: number;
  description: string;
  characteristics: Record<string, any>;
  advantages: string[];
  disadvantages: string[];
}

export interface OptimizationRecommendation {
  type: 'TEACHER_ADJUSTMENT' | 'ROOM_CHANGE' | 'SCHEDULE_MODIFICATION';
  title: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  expectedImprovement: number;
  actionSteps: string[];
  parameters: Record<string, any>;
}

// Teacher Workload Analysis
export interface TeacherWorkloadAnalysis {
  teacherId: number;
  teacherFirstName: string;
  teacherLastName: string;
  teacherEmail: string;
  
  // Workload Metrics
  totalWeeklyHours: number;
  maxWeeklyCapacity: number;
  workloadPercentage: number;
  status: WorkloadStatus;
  
  // Daily Breakdown
  dailyWorkloads: Record<string, DailyWorkload>; // day -> workload
  
  // Schedule Quality
  totalGaps: number;
  maxConsecutiveHours: number;
  scheduleEfficiency: number;
  scheduleIssues: string[];
  
  // Course Distribution
  courseWorkloads: CourseWorkload[];
  totalCourses: number;
  totalClasses: number;
  
  // Room Usage
  roomUsages: RoomUsage[];
  totalRoomsUsed: number;
  
  // Recommendations
  recommendations: WorkloadRecommendation[];
  
  // Comparison Metrics
  averageWorkloadComparison: number; // compared to other teachers
  workloadRanking: 'TOP_25' | 'AVERAGE' | 'BOTTOM_25';
}

export type WorkloadStatus = 
  | 'OPTIMAL'           // 80-100% capacity
  | 'UNDERUTILIZED'     // < 80% capacity
  | 'OVERLOADED'        // > 100% capacity
  | 'SEVERELY_OVERLOADED'; // > 120% capacity

export interface DailyWorkload {
  dayOfWeek: string;
  hoursScheduled: number;
  gaps: number;
  consecutiveHours: number;
  timeSlots: TimeSlot[];
  efficiency: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface TimeSlot {
  period: string;
  courseName: string;
  className: string;
  roomName: string;
  isConsecutive: boolean;
  isPreferredTime: boolean;
}

export interface CourseWorkload {
  courseId: number;
  courseName: string;
  courseCode: string;
  weeklyHours: number;
  numberOfClasses: number;
  classNames: string[];
  courseComplexity: number; // based on course difficulty
}

export interface RoomUsage {
  roomId: number;
  roomName: string;
  hoursUsed: number;
  utilizationRate: number;
  isOptimalRoom: boolean; // based on course requirements
}

export interface WorkloadRecommendation {
  type: 'REDUCE_LOAD' | 'OPTIMIZE_SCHEDULE' | 'CHANGE_ROOMS' | 'INCREASE_LOAD';
  title: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  expectedImprovement: number;
  actionSteps: string[];
  parameters: Record<string, any>;
}

// Conflict Report
export interface TimetableConflictReport {
  timetableId: number;
  timetableName: string;
  analysisDate: string;
  overallSeverity: ConflictSeverity;
  
  // Conflict Summary
  totalConflicts: number;
  criticalConflicts: number;
  majorConflicts: number;
  minorConflicts: number;
  
  // Conflict Categories
  teacherConflicts: TeacherConflict[];
  roomConflicts: RoomConflict[];
  classConflicts: ClassConflict[];
  capacityConflicts: CapacityConflict[];
  preferenceViolations: PreferenceViolation[];
  
  // Impact Analysis
  impactAnalysis: ConflictImpactAnalysis;
  
  // Resolution Suggestions
  resolutionSuggestions: ConflictResolutionSuggestion[];
  
  // Statistics
  conflictTypeStatistics: Record<string, number>;
  conflictSeverityDistribution: Record<string, number>;
}

export type ConflictSeverity = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface TeacherConflict {
  teacherId: number;
  teacherName: string;
  conflictType: 'DOUBLE_BOOKING' | 'OVERLOAD' | 'PREFERENCE_VIOLATION';
  description: string;
  conflictingSlots: ConflictSlot[];
  severity: ConflictSeverity;
  impact: string;
  resolutionOptions: string[];
}

export interface RoomConflict {
  roomId: number;
  roomName: string;
  conflictType: 'DOUBLE_BOOKING' | 'CAPACITY_EXCEEDED' | 'EQUIPMENT_MISMATCH';
  description: string;
  conflictingSlots: ConflictSlot[];
  severity: ConflictSeverity;
  impact: string;
  resolutionOptions: string[];
}

export interface ClassConflict {
  classId: number;
  className: string;
  conflictType: 'DOUBLE_BOOKING' | 'EXCESSIVE_HOURS' | 'INSUFFICIENT_BREAKS';
  description: string;
  conflictingSlots: ConflictSlot[];
  severity: ConflictSeverity;
  impact: string;
  resolutionOptions: string[];
}

export interface CapacityConflict {
  roomId: number;
  roomName: string;
  classId: number;
  className: string;
  roomCapacity: number;
  classSize: number;
  capacityDeficit: number;
  severity: ConflictSeverity;
  resolutionOptions: string[];
}

export interface PreferenceViolation {
  violationType: 'TEACHER_PREFERENCE' | 'TIME_PREFERENCE' | 'ROOM_PREFERENCE';
  description: string;
  affectedEntityId: number; // teacher, class, or course ID
  affectedEntityName: string;
  preferenceDetails: string;
  severity: ConflictSeverity;
  impactScore: number;
}

export interface ConflictSlot {
  slotId: number;
  dayOfWeek: string;
  period: string;
  courseName: string;
  className: string;
  teacherName: string;
  roomName: string;
  conflictReason: string;
}

export interface ConflictImpactAnalysis {
  affectedTeachers: number;
  affectedClasses: number;
  affectedRooms: number;
  affectedStudents: number; // estimated
  scheduleQualityScore: number;
  teacherSatisfactionScore: number;
  studentSatisfactionScore: number;
  majorImpacts: string[];
}

export interface ConflictResolutionSuggestion {
  suggestionId: string;
  title: string;
  description: string;
  resolutionType: 'AUTOMATIC' | 'MANUAL' | 'HYBRID';
  targetSeverity: ConflictSeverity;
  expectedImprovement: number;
  estimatedTimeMinutes: number;
  steps: string[];
  parameters: Record<string, any>;
  prerequisites: string[];
  consequences: string[];
}

// Enhanced Timetable Slot for UI
export interface EnhancedTimetableSlot {
  id: number;
  timetableId: number;
  dayOfWeek: string;
  period: {
    id: number;
    name: string;
    startTime: string;
    endTime: string;
    index: number;
  };
  
  // Assignment Details
  forClass?: {
    id: number;
    name: string;
    yearOfStudy: number;
    maxStudents: number;
  };
  
  forCourse?: {
    id: number;
    name: string;
    code: string;
    credit: number;
    difficultyLevel?: number;
  };
  
  teacher?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    weeklyCapacity: number;
  };
  
  room?: {
    id: number;
    name: string;
    capacity: number;
    equipment?: string[];
  };
  
  description?: string;
  
  // UI State
  isSelected?: boolean;
  isDragging?: boolean;
  hasConflict?: boolean;
  conflictType?: string;
  isOptimal?: boolean;
  
  // Metadata
  createdAt: string;
  updatedAt: string;
}

// Timetable Grid Configuration
export interface TimetableGridConfig {
  showWeekends: boolean;
  startHour: number;
  endHour: number;
  slotDuration: number; // in minutes
  showRoomInfo: boolean;
  showTeacherInfo: boolean;
  colorScheme: 'subject' | 'teacher' | 'room' | 'class';
  enableDragDrop: boolean;
  enableConflictHighlight: boolean;
  compactView: boolean;
}

// Drag and Drop Types
export interface DragItem {
  type: 'TIMETABLE_SLOT';
  slot: EnhancedTimetableSlot;
  sourcePosition: {
    day: string;
    periodId: number;
  };
}

export interface DropResult {
  targetDay: string;
  targetPeriodId: number;
  targetRoomId?: number;
  targetTeacherId?: number;
}

// Analytics Types
export interface TimetableAnalytics {
  conflictReport: TimetableConflictReport;
  workloadAnalyses: TeacherWorkloadAnalysis[];
  overallHealth: 'EXCELLENT' | 'GOOD' | 'NEEDS_ATTENTION';
  utilizationStats: {
    averageTeacherUtilization: number;
    averageRoomUtilization: number;
    totalScheduledHours: number;
    efficiencyScore: number;
  };
  recommendations: {
    priority: 'HIGH' | 'MEDIUM' | 'LOW';
    category: string;
    title: string;
    description: string;
    impact: number;
  }[];
}

// Filter and Search Types
export interface TimetableFilters {
  teacherIds?: number[];
  classIds?: number[];
  courseIds?: number[];
  roomIds?: number[];
  dayOfWeek?: string[];
  conflictsOnly?: boolean;
  unassignedOnly?: boolean;
  searchQuery?: string;
}

// Export Types
export interface TimetableExportOptions {
  format: 'PDF' | 'EXCEL' | 'CSV';
  includeTeacherInfo: boolean;
  includeRoomInfo: boolean;
  includeConflicts: boolean;
  dateRange?: {
    startDate: string;
    endDate: string;
  };
  customTitle?: string;
  orientation: 'portrait' | 'landscape';
}
