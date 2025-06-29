/* eslint-disable @typescript-eslint/no-explicit-any */
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  PaginationState,
  SortingState,
  useReactTable,
  RowSelectionState,
} from "@tanstack/react-table";
import { cn } from "@/lib/utils";
import {
  ChevronsLeft,
  ChevronLeft,
  ChevronRight,
  ChevronsRight,
  ArrowUpDown,
  Search,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useState } from "react";

export interface ServerTableState {
  pagination: PaginationState;
  sorting: SortingState;
  search?: string;
  rowSelection?: RowSelectionState;
}

interface DataTableProps<TData> {
  data: TData[];
  columns: ColumnDef<TData, any>[];
  total: number;
  state: ServerTableState;
  onStateChange: (state: ServerTableState) => void;
  loading?: boolean;
  emptyLabel?: string;
  searchPlaceholder?: string;
  enableRowSelection?: boolean;
}

export function DataTable<TData>({
  data,
  columns,
  total,
  state,
  onStateChange,
  loading,
  emptyLabel = "No data",
  searchPlaceholder = "Search...",
  enableRowSelection = false,
}: DataTableProps<TData>) {
  const [searchValue, setSearchValue] = useState(state.search || "");

  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    manualPagination: true,
    manualSorting: true,
    pageCount: Math.ceil(total / state.pagination.pageSize),
    state: {
      pagination: state.pagination,
      sorting: state.sorting,
      rowSelection: state.rowSelection || {},
    },
    onPaginationChange: (updater) => {
      const newPagination = typeof updater === "function" ? updater(state.pagination) : updater;
      onStateChange({
        ...state,
        pagination: newPagination,
      });
    },
    onSortingChange: (updater) => {
      const newSorting = typeof updater === "function" ? updater(state.sorting) : updater;
      onStateChange({
        ...state,
        sorting: newSorting,
      });
    },
    onRowSelectionChange: enableRowSelection
      ? (updater) => {
          const newRowSelection = typeof updater === "function" ? updater(state.rowSelection || {}) : updater;
          onStateChange({
            ...state,
            rowSelection: newRowSelection,
          });
        }
      : undefined,
    enableRowSelection,
  });

  const handleSearch = () => {
    onStateChange({
      ...state,
      search: searchValue,
      pagination: { ...state.pagination, pageIndex: 0 },
    });
  };

  const handleSearchKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  };

  return (
    <div className="space-y-6">
      {/* Search Bar */}
      <div className="flex items-center space-x-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input
            placeholder={searchPlaceholder}
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            onKeyPress={handleSearchKeyPress}
            className="pl-10 border-slate-200/80 focus:border-blue-300 transition-colors duration-200"
          />
        </div>
        <Button 
          onClick={handleSearch} 
          variant="outline"
          className="bg-gradient-to-r from-blue-50 to-indigo-50 hover:from-blue-100 hover:to-indigo-100 border-blue-200/60 transition-all duration-200"
        >
          Search
        </Button>
      </div>

      {/* Selection Info */}
      {enableRowSelection && Object.keys(state.rowSelection || {}).length > 0 && (
        <div className="text-sm text-slate-600 bg-blue-50/80 px-3 py-2 rounded-lg border border-blue-200/60">
          {Object.keys(state.rowSelection || {}).length} of {table.getFilteredRowModel().rows.length} row(s) selected
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-slate-200/60 shadow-lg bg-white">
        <table className="w-full text-sm">
          <thead className="bg-gradient-to-r from-slate-50/80 to-blue-50/30">
            {table.getHeaderGroups().map((hg) => (
              <tr key={hg.id} className="border-b border-slate-200/80">
                {hg.headers.map((header) => {
                  const canSort = header.column.getCanSort();
                  return (
                    <th
                      key={header.id}
                      className={cn(
                        "px-4 py-4 text-left font-semibold text-slate-700 transition-colors duration-200",
                        canSort && "cursor-pointer select-none hover:text-slate-900"
                      )}
                      onClick={canSort ? header.column.getToggleSortingHandler() : undefined}
                    >
                      <div className="flex items-center space-x-1">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {canSort && (
                          <ArrowUpDown
                            className={cn(
                              "h-3 w-3 transition-all duration-200",
                              header.column.getIsSorted() && header.column.getIsSorted() !== false &&
                                header.column.getIsSorted() === "asc"
                                ? "rotate-180 text-blue-600"
                                : header.column.getIsSorted() === "desc"
                                ? "text-blue-600"
                                : "text-slate-400"
                            )}
                          />
                        )}
                      </div>
                    </th>
                  );
                })}
              </tr>
            ))}
          </thead>
          
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={columns.length} className="p-8 text-center">
                  <div className="flex items-center justify-center space-x-2">
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
                    <span className="text-slate-600">Loading...</span>
                  </div>
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="p-8 text-center text-slate-500">
                  <div className="flex flex-col items-center justify-center space-y-3">
                    <div className="h-12 w-12 rounded-full bg-slate-100 flex items-center justify-center">
                      <span className="text-slate-400 text-xl">📋</span>
                    </div>
                    <div>
                      <p className="font-medium">{emptyLabel}</p>
                      <p className="text-sm text-slate-400">Try adjusting your search criteria</p>
                    </div>
                  </div>
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row, index) => (
                <tr 
                  key={row.id} 
                  className={cn(
                    "border-t border-slate-100/80 transition-colors duration-200 ease-in-out group cursor-pointer",
                    "hover:bg-gradient-to-r hover:from-blue-50/60 hover:via-indigo-50/30 hover:to-purple-50/20",
                    row.getIsSelected() && "bg-gradient-to-r from-blue-100/70 via-indigo-100/40 to-purple-100/30 shadow-sm",
                    index % 2 === 0 ? "bg-white" : "bg-slate-50/30"
                  )}
                >
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-4 py-4 text-slate-700 group-hover:text-slate-900 transition-colors duration-200">
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Controls */}
      <div className="flex items-center justify-between px-2">
        <div className="text-sm text-slate-600">
          Showing {state.pagination.pageIndex * state.pagination.pageSize + 1} to{" "}
          {Math.min((state.pagination.pageIndex + 1) * state.pagination.pageSize, total)} of {total} entries
        </div>
        
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.setPageIndex(0)}
            disabled={!table.getCanPreviousPage()}
            className="h-8 w-8 p-0 hover:bg-blue-50 transition-colors duration-200"
          >
            <ChevronsLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}
            className="h-8 w-8 p-0 hover:bg-blue-50 transition-colors duration-200"
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          
          <div className="flex items-center space-x-1">
            <span className="text-sm text-slate-600">Page</span>
            <span className="text-sm font-semibold text-slate-900">
              {state.pagination.pageIndex + 1} of {table.getPageCount()}
            </span>
          </div>
          
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}
            className="h-8 w-8 p-0 hover:bg-blue-50 transition-colors duration-200"
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.setPageIndex(table.getPageCount() - 1)}
            disabled={!table.getCanNextPage()}
            className="h-8 w-8 p-0 hover:bg-blue-50 transition-colors duration-200"
          >
            <ChevronsRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
} 