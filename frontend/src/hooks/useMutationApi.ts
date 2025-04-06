import {
    useMutation,
    UseMutationOptions,
    UseMutationResult,
  } from "@tanstack/react-query";
  import { AxiosError } from "axios";
  
  /**
   * Typed wrapper around `useMutation` (React‑Query v5).
   *
   * Generic order in v5: <TData, TError, TVariables, TContext>
   */
  export function useMutationApi<
    TData = unknown,
    TVars = void,
    TContext = unknown,
  >(
    mutationFn: (vars: TVars) => Promise<TData>,
    options?: Omit<
      UseMutationOptions<TData, AxiosError, TVars, TContext>,
      "mutationFn"
    >,
  ): UseMutationResult<TData, AxiosError, TVars, TContext> {
    return useMutation<TData, AxiosError, TVars, TContext>({
      mutationFn,
      ...options,
    });
  }
  