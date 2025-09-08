import type { JoinOperator } from "@/types/data-table";

/**
 * Data table configuration
 */
export const dataTableConfig = {
  /**
   * Available sort orders for data table sorting
   */
  sortOrders: [
    {
      label: "Ascending",
      value: "asc",
    },
    {
      label: "Descending", 
      value: "desc",
    },
  ],

  /**
   * Available join operators for combining filters
   */
  joinOperators: ["and", "or"] as JoinOperator[],

  /**
   * Default pagination settings
   */
  pagination: {
    defaultPageSize: 10,
    pageSizeOptions: [10, 20, 30, 40, 50],
  },

  /**
   * Default table features
   */
  features: {
    enableAdvancedFilter: true,
    enableRowSelection: true,
    enableColumnVisibility: true,
    enableSorting: true,
    enablePagination: true,
    enableBulkActions: true,
  },

  /**
   * Filter configuration
   */
  filters: {
    debounceMs: 300,
    maxFilters: 10,
  },

  /**
   * Sorting configuration
   */
  sorting: {
    maxSorts: 5,
  },
} as const;

export type DataTableConfig = typeof dataTableConfig;
