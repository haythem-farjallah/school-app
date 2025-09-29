import { useState } from "react";
import { useQueryApi } from "./useQueryApi";
import { http } from "@/lib/http";
import type { ApiResponse, Page, PageDto } from "@/types/api";
import type { AxiosRequestConfig } from "axios";

export function usePaginated<T>(
  endpoint: string,
  queryKey: string,
  limit: number = 10,
  params: Record<string, any> = {},
  externalPage?: number,
  skipAuth: boolean = false,
) {
  const [internalPage, setInternalPage] = useState(0);
  const currentPage = externalPage !== undefined ? externalPage : internalPage;

  /* ------------- fetcher ------------------------------------------------ */
  const  fetchPage = async(p: number): Promise<Page<T>>  => {
    const cfg: AxiosRequestConfig & { skipAuth?: boolean } = {
      params: { page: p, size: limit, ...params },
    };
    if (skipAuth) cfg.skipAuth = true;

   return http
      .get<ApiResponse<PageDto<T>>>(endpoint, cfg)
      .then((response) => {
        // The http interceptor unwraps the axios response at runtime
        // TypeScript sees AxiosResponse but runtime gets ApiResponse due to interceptor
        const apiResponse = response as unknown as ApiResponse<PageDto<T>>;
        const dto: PageDto<T> = apiResponse.data;
        
        return {
          data: dto.content,
          totalElements: dto.totalElements,
          totalPages: dto.totalPages,
          currentPage: dto.number,
          size: dto.size,
          hasNext: !dto.last,
          hasPrevious: !dto.first,
        };
      });
  };

  /* ------------- query --------------------------------------------------- */
  const queryKeyArray = [queryKey, currentPage, limit, params];
  
  const query = useQueryApi<Page<T>>(
    queryKeyArray,
    () => fetchPage(currentPage),
    {  
      placeholderData: (prev) => prev,
      // Disable caching for learning resources to always get fresh data
      cacheTime: queryKey === "learning-resources" ? 0 : undefined,
      staleTime: queryKey === "learning-resources" ? 0 : undefined,
    },
  );

  /* ------------- pagination helpers -------------------------------------- */
  const goToPage = (page: number) => {
    if (externalPage === undefined) {
      setInternalPage(page);
    }
  };

  const nextPage = () => {
    if (query.data?.hasNext) {
      goToPage(currentPage + 1);
    }
  };

  const prevPage = () => {
    if (query.data?.hasPrevious) {
      goToPage(currentPage - 1);
    }
  };

  return {
    ...query,
    currentPage,
    goToPage,
    nextPage,
    prevPage,
  };
}

