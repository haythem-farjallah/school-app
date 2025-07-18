import { useState } from "react";
import { http } from "../lib/http";
import { useQueryApi } from "./useQueryApi";
import type { AxiosRequestConfig } from "axios";
import { PageDto, ApiResponse } from "@/types/level";

export interface Page<T> {
  data: T[];
  page: number;
  totalPages: number;
  totalItems: number;
}

export function usePaginated<T>(
  endpoint: string,
  queryKey: unknown,
  limit = 10,
  params: Record<string, unknown> = {},
  skipAuth = false,
  externalPage?: number, // Add external page parameter
) {
  const [internalPage, setInternalPage] = useState(0);
  
  // Use external page if provided, otherwise use internal page
  const currentPage = externalPage !== undefined ? externalPage : internalPage;

  /* ------------- fetcher ------------------------------------------------ */
  const  fetchPage = async(p: number): Promise<Page<T>>  => {
    console.log("🌐 usePaginated - Fetching page:", p, "for endpoint:", endpoint);
    const cfg: AxiosRequestConfig & { skipAuth?: boolean } = {
      params: { page: p, size: limit, ...params },
    };
    if (skipAuth) cfg.skipAuth = true;

   return http
      .get<ApiResponse<PageDto<T>>>(endpoint, cfg)
      .then((response) => {
        console.log("✅ usePaginated - API response received for page:", p);
        
        // The http interceptor unwraps the axios response at runtime
        // TypeScript sees AxiosResponse but runtime gets ApiResponse due to interceptor
        const apiResponse = response as unknown as ApiResponse<PageDto<T>>;
        const dto: PageDto<T> = apiResponse.data;
        
        return {
          data: dto.content,
          page: dto.page,
          totalPages: Math.ceil(dto.totalElements / dto.size),
          totalItems: dto.totalElements,
        };
      });
  };

  /* ------------- react‑query ------------------------------------------- */
  const queryKeyArray = [queryKey, currentPage, limit, params, skipAuth];

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
