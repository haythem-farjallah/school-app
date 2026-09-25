import { useState } from "react";
import { api } from "@/lib/api-client";
import { useQueryApi } from "./useQueryApi";
import type { ApiResponse, PageDto } from "@/types/level";

export interface Page<T> {
  data: T[];
  page: number;
  totalPages: number;
  totalItems: number;
}

/**
 * Pages through a list endpoint that answers ApiResponse<PageDto<T>>.
 * Failures are exposed as the query's error / isError; the calling UI decides
 * how to present them.
 */
export function usePaginated<T>(
  endpoint: string,
  queryKey: unknown,
  limit = 10,
  params: Record<string, unknown> = {},
  externalPage?: number, // Add external page parameter
) {
  const [internalPage, setInternalPage] = useState(0);
  
  // Use external page if provided, otherwise use internal page
  const currentPage = externalPage !== undefined ? externalPage : internalPage;

  /* ------------- fetcher ------------------------------------------------ */
  const fetchPage = async (p: number): Promise<Page<T>> => {
    const response = await api.get<ApiResponse<PageDto<T>>>(endpoint, {
      params: { page: p, size: limit, ...params },
    });
    const dto = response.data.data;

    return {
      data: dto.content,
      page: dto.page,
      totalPages: Math.ceil(dto.totalElements / dto.size),
      totalItems: dto.totalElements,
    };
  };

  /* ------------- react‑query ------------------------------------------- */
  const queryKeyArray = [queryKey, currentPage, limit, params];

  const query = useQueryApi<Page<T>>(
    queryKeyArray, // Use currentPage in query key
    () => fetchPage(currentPage), // Use currentPage for fetching
    {  placeholderData: (prev) => prev, },
  );

  /* ------------- helpers ------------------------------------------------ */
  const loadMore = () => {
    if (query.data && currentPage + 1 < query.data.totalPages) {
      if (externalPage !== undefined) {
        // If external page is provided, we can't control pagination internally
        console.warn("Cannot use loadMore when external page is provided");
      } else {
        setInternalPage((prev) => prev + 1);
      }
    }
  };

  const setPage = (page: number) => {
    if (externalPage !== undefined) {
      // If external page is provided, we can't control pagination internally
      console.warn("Cannot use setPage when external page is provided");
    } else {
      setInternalPage(page);
    }
  };

  return { ...query, page: currentPage, setPage, loadMore, fetchPage };
}
