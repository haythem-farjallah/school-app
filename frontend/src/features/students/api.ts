import { api } from "@/lib/api-client";
import type { ApiResponse, PageDto } from "@/types/level";
import type { CreateStudentData, Student, UpdateStudentData } from "@/types/student";

export interface StudentPageRequest {
  page: number;
  size: number;
}

/** GET /v1/students — one page of students, unfiltered. */
export async function listStudents({ page, size }: StudentPageRequest): Promise<PageDto<Student>> {
  const response = await api.get<ApiResponse<PageDto<Student>>>("/v1/students", { params: { page, size } });
  return response.data.data;
}

/** GET /v1/students/search — one page of students whose name or email contains `q`. */
export async function searchStudents({ q, page, size }: StudentPageRequest & { q: string }): Promise<PageDto<Student>> {
  const response = await api.get<ApiResponse<PageDto<Student>>>("/v1/students/search", {
    params: { q, page, size },
  });
  return response.data.data;
}

export async function getStudent(id: number): Promise<Student> {
  const response = await api.get<ApiResponse<Student>>(`/v1/students/${id}`);
  return response.data.data;
}

export async function createStudent(data: CreateStudentData): Promise<Student> {
  const response = await api.post<ApiResponse<Student>>("/v1/students", data);
  return response.data.data;
}

export async function updateStudent({ id, firstName, lastName, gradeLevel, enrollmentYear }: UpdateStudentData): Promise<Student> {
  const response = await api.patch<ApiResponse<Student>>(`/v1/students/${id}`, {
    firstName,
    lastName,
    gradeLevel,
    enrollmentYear,
  });
  return response.data.data;
}

export async function deleteStudent(id: number): Promise<void> {
  await api.delete(`/v1/students/${id}`);
}
