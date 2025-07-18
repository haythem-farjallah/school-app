export interface Class {
  id: number;
  name: string;
  yearOfStudy: number;
  maxStudents: number;
  studentIds: number[];
  courseIds: number[];
  teacherIds: number[];
  assignedRoomId: number | null;
}

export interface CreateClassRequest {
  name: string;
  yearOfStudy: number;
  maxStudents: number;
}

export interface UpdateClassRequest {
  name: string;
  yearOfStudy: number;
  maxStudents: number;
}

export interface ClassFilters {
  name?: string;
  yearOfStudy?: number;
  maxStudents?: number;
}

export interface ClassResponse {
  content: Class[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ClassAssignmentRequest {
  classId: number;
  studentIds?: number[];
  courseIds?: number[];
  teacherIds?: number[];
  assignedRoomId?: number | null;
}

export interface ClassStatistics {
  totalClasses: number;
  totalStudents: number;
  averageClassSize: number;
  classesWithRooms: number;
  classesWithoutRooms: number;
} 