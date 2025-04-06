import {
    useQuery,
    UseQueryOptions,
    UseQueryResult,
  } from "@tanstack/react-query";
  import { AxiosError } from "axios";
  
  /**
   * Generic wrapper that:
   *   • fixes the generic order for v5
   *   • defaults the error type to AxiosError
   */
  export function useQueryApi<
    TData,
    TQueryKey extends readonly unknown[] = readonly unknown[],
  >(
    queryKey: TQueryKey,
    queryFn: () => Promise<TData>,
    options?: Omit<
      UseQueryOptions<TData, AxiosError, TData, TQueryKey>,
      "queryKey" | "queryFn"
    >,
  ): UseQueryResult<TData, AxiosError> {
    return useQuery<TData, AxiosError, TData, TQueryKey>({
      queryKey,
      queryFn,
      ...options,
    });
  }
  