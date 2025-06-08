/**
 * Extended column sort type
 */
export interface ExtendedColumnSort {
  id: string;
  desc: boolean;
}

/**
 * Extended column filter type
 */
export interface ExtendedColumnFilter<TData> {
  id: Extract<keyof TData, string>;
  value: unknown;
  variant?: "text" | "number" | "date" | "dateRange" | "select" | "multiSelect" | "range";
  operator?: FilterOperator;
  filterId: string;
}

/**
 * Filter operator type
 */
export type FilterOperator = 
  | "contains" 
  | "equals" 
  | "startsWith" 
  | "endsWith" 
  | "isEmpty" 
  | "isNotEmpty"
  | "greaterThan"
  | "lessThan"
  | "between";

/**
 * Join operator type
 */
export type JoinOperator = "and" | "or";

/**
 * Data table configuration
 */
export interface DataTableConfig {
  enableAdvancedFilter?: boolean;
  enableRowSelection?: boolean;
  enableColumnVisibility?: boolean;
  enableSorting?: boolean;
  enablePagination?: boolean;
}

/**
 * Column filter configuration
 */
export interface ColumnFilterConfig {
  variant?: "text" | "number" | "date" | "dateRange" | "select" | "multiSelect" | "range";
  label?: string;
  placeholder?: string;
  options?: Array<{ label: string; value: string; icon?: React.ComponentType; count?: number }>;
  unit?: string;
  range?: [number, number];
}

/**
 * Data table state
 */
export interface DataTableState {
  pagination: {
    pageIndex: number;
    pageSize: number;
  };
  sorting: ExtendedColumnSort[];
  columnFilters: Array<{
    id: string;
    value: unknown;
  }>;
  columnVisibility: Record<string, boolean>;
  rowSelection: Record<string, boolean>;
}

/**
 * Data table search params
 */
export interface DataTableSearchParams {
  page?: string;
  perPage?: string;
  sort?: string;
  filters?: string;
  visibility?: string;
  selection?: string;
}

// Module declaration to extend react-table types
declare module "@tanstack/react-table" {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface ColumnMeta<TData, TValue> {
    variant?: "text" | "number" | "date" | "dateRange" | "select" | "multiSelect" | "range";
    label?: string;
    placeholder?: string;
    options?: Array<{ label: string; value: string; icon?: React.ComponentType; count?: number }>;
    unit?: string;
    range?: [number, number];
  }
} 