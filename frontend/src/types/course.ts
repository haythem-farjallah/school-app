export interface Course {
  id: number;
  name: string;
  color: string;
  credit: number;
  weeklyCapacity: number;
  teacherId: number;
}

export interface CoursesResponse {
  content: Course[];
  page: number;
  size: number;
  totalElements: number;
} 