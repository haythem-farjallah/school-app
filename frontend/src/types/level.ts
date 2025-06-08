export interface Level {
  id: number;
  name: string;
  courseIds: number[];
}

export interface Class {
  id: number;
  name: string;
  levelId: number;
  studentIds: number[];
  courseIds: number[];
  scheduleId: number | null;
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