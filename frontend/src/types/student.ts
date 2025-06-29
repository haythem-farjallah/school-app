export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string | null;
  birthday?: string | null;
  gender?: string | null;
  address?: string | null;
  gradeLevel: string;
  enrollmentYear: number;
}

export interface CreateStudentData {
  profile: {
    firstName: string;
    lastName: string;
    email: string;
    telephone?: string | null;
    birthday?: string | null;
    gender?: string | null;
    address?: string | null;
  };
  gradeLevel: string;
  enrollmentYear: number;
}

export interface StudentsResponse {
  content: Student[];
  page: number;
  size: number;
  totalElements: number;
} 