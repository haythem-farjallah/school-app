import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { AxiosError } from "axios";
import type { Page } from "@/hooks/usePaginated";
import type { CreateStudentData, Student, UpdateStudentData } from "@/types/student";
import { createStudent, deleteStudent, getStudent, listStudents, searchStudents, updateStudent } from "../api";

export const studentKeys = {
  all: ["students"] as const,
  lists: () => [...studentKeys.all, "list"] as const,
  list: (params: { page: number; size: number; search: string }) => [...studentKeys.lists(), params] as const,
  detail: (id: number) => [...studentKeys.all, "detail", id] as const,
};

/**
 * One page of students. A non-blank `search` uses the backend search endpoint
 * (name or email); otherwise the plain list endpoint.
 */
export function useStudents({ page = 0, size = 10, search = "" }: { page?: number; size?: number; search?: string } = {}) {
  const q = search.trim();

  return useQuery<Page<Student>, AxiosError>({
    queryKey: studentKeys.list({ page, size, search: q }),
    queryFn: async () => {
      const dto = q ? await searchStudents({ q, page, size }) : await listStudents({ page, size });
      return {
        data: dto.content,
        page: dto.page,
        totalPages: Math.ceil(dto.totalElements / dto.size),
        totalItems: dto.totalElements,
      };
    },
    placeholderData: keepPreviousData,
  });
}

export function useStudent(id?: number) {
  return useQuery<Student, AxiosError>({
    queryKey: studentKeys.detail(id ?? 0),
    queryFn: () => getStudent(id!),
    enabled: id !== undefined && Number.isFinite(id),
  });
}

export function useCreateStudent() {
  const queryClient = useQueryClient();

  return useMutation<Student, AxiosError, CreateStudentData>({
    mutationFn: createStudent,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: studentKeys.lists() }),
  });
}

export function useUpdateStudent() {
  const queryClient = useQueryClient();

  return useMutation<Student, AxiosError, UpdateStudentData>({
    mutationFn: updateStudent,
    onSuccess: (student) => {
      // The PATCH response is the updated student, so the detail needs no refetch.
      queryClient.setQueryData(studentKeys.detail(student.id), student);
      return queryClient.invalidateQueries({ queryKey: studentKeys.lists() });
    },
  });
}

export function useDeleteStudent() {
  const queryClient = useQueryClient();

  return useMutation<void, AxiosError, number>({
    mutationFn: deleteStudent,
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: studentKeys.detail(id) });
      return queryClient.invalidateQueries({ queryKey: studentKeys.lists() });
    },
  });
}
