export interface Course {
  id: number;
  name: string;
  color: string;
  coefficient: number;
  teacherId: number;
}

export interface CoursesResponse {
  content: Course[];
  page: number;
  size: number;
  totalElements: number;
} 