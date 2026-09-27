export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string | null;
  birthday?: string | null;
  gender?: string | null;
  address?: string | null;
  gradeLevel?: string | null;
  enrollmentYear?: number | null;
}

/** Body of POST /v1/students. */
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

/** PATCH /v1/students/{id} changes only these fields; profile contact details are not updatable there. */
export interface UpdateStudentData {
  id: number;
  firstName: string;
  lastName: string;
  gradeLevel: string;
  enrollmentYear: number;
}

export interface StudentsResponse {
  content: Student[];
  page: number;
  size: number;
  totalElements: number;
}
