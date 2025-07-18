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

export interface Room {
  id: number;
  name: string;
  capacity: number;
  roomType: "CLASSROOM" | "LABORATORY" | "CONFERENCE" | "AUDITORIUM" | "GYM" | "LIBRARY" | "OTHER";
}

export interface Staff {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone: string;
  birthday: string;
  gender: string;
  address: string;
  staffType: "MAINTENANCE" | "SECURITY" | "ADMINISTRATIVE" | "TECHNICAL" | "OTHER";
  department: string;
}

export interface ApiResponse<T> {
    status: string;
    data: T;
}

export interface PageDto<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
} 