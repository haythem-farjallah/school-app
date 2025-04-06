// src/hooks/usePaginated.ts
import { useState } from "react";
import { http } from "../lib/http";
import { useQueryApi } from "./useQueryApi";
import type { AxiosRequestConfig } from "axios";

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
) {
  const [page, setPage] = useState(0);

  /* ------------- fetcher ------------------------------------------------ */
  const fetchPage = (p: number) => {
    const cfg: AxiosRequestConfig & { skipAuth?: boolean } = {
      params: { page: p, size: limit, ...params },
    };
    if (skipAuth) cfg.skipAuth = true;

    return http.get<Page<T>>(endpoint, cfg).then((res) => res.data);
  };

  /* ------------- react‑query ------------------------------------------- */
  const query = useQueryApi<Page<T>>(
    [queryKey, page, limit, params, skipAuth],
    () => fetchPage(page),
    { keepPreviousData: true },
  );

  /* ------------- helpers ------------------------------------------------ */
  const loadMore = () => {
    if (query.data && page + 1 < query.data.totalPages) {
      setPage((prev) => prev + 1);
    }
  };

  return { ...query, page, setPage, loadMore, fetchPage };
}
