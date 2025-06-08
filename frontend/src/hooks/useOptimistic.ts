import { QueryClient } from "@tanstack/react-query";

export const useOptimistic = (qc: QueryClient) => {
  /**
   * Mutates the cache immediately, returns a rollback fn.
   * Call the rollback in `onError`.
   */
  return <T>(
    key: unknown[],
    updater: (current: T | undefined) => T,
  ) => {
    const prev = qc.getQueryData<T>(key);
    qc.setQueryData<T>(key, updater(prev));
    return () => qc.setQueryData(key, prev);
  };
};
