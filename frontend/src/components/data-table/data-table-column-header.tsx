"use client";

import type { Column } from "@tanstack/react-table";
import {
  ChevronDown,
  ChevronUp,
  ChevronsUpDown,
  EyeOff,
  X,
} from "lucide-react";

import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

interface DataTableColumnHeaderProps<TData, TValue>
  extends React.ComponentProps<typeof DropdownMenuTrigger> {
  column: Column<TData, TValue>;
  title: string;
}

export function DataTableColumnHeader<TData, TValue>({
  column,
  title,
  className,
  ...props
}: DataTableColumnHeaderProps<TData, TValue>) {
  if (!column.getCanSort() && !column.getCanHide()) {
    return <div className={cn("font-semibold text-slate-700", className)}>{title}</div>;
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className={cn(
          "-ml-1.5 flex h-8 items-center gap-1.5 rounded-lg px-3 py-2 font-semibold text-slate-700",
          "hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:text-blue-900",
          "focus:outline-none focus:ring-2 focus:ring-blue-300/60 focus:bg-gradient-to-r focus:from-blue-50 focus:to-indigo-50",
          "data-[state=open]:bg-gradient-to-r data-[state=open]:from-blue-100 data-[state=open]:to-indigo-100 data-[state=open]:text-blue-900",
          "transition-all duration-200 [&_svg]:size-4 [&_svg]:shrink-0",
          className,
        )}
        {...props}
      >
        {title}
        {column.getCanSort() &&
          (column.getIsSorted() === "desc" ? (
            <ChevronDown className="text-blue-600" />
          ) : column.getIsSorted() === "asc" ? (
            <ChevronUp className="text-blue-600" />
          ) : (
            <ChevronsUpDown className="text-slate-500" />
          ))}
      </DropdownMenuTrigger>
      <DropdownMenuContent 
        align="start" 
        className="w-40 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl rounded-xl"
      >
        {column.getCanSort() && (
          <>
            <DropdownMenuCheckboxItem
              className="relative pr-8 pl-3 py-2.5 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-blue-50 focus:to-indigo-50 [&>span:first-child]:right-2 [&>span:first-child]:left-auto"
              checked={column.getIsSorted() === "asc"}
              onClick={() => column.toggleSorting(false)}
            >
              <ChevronUp className="text-blue-600" />
              <span className="font-medium">Ascending</span>
            </DropdownMenuCheckboxItem>
            <DropdownMenuCheckboxItem
              className="relative pr-8 pl-3 py-2.5 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-blue-50 focus:to-indigo-50 [&>span:first-child]:right-2 [&>span:first-child]:left-auto"
              checked={column.getIsSorted() === "desc"}
              onClick={() => column.toggleSorting(true)}
            >
              <ChevronDown className="text-blue-600" />
              <span className="font-medium">Descending</span>
            </DropdownMenuCheckboxItem>
            {column.getIsSorted() && (
              <DropdownMenuItem
                className="pl-3 py-2.5 hover:bg-gradient-to-r hover:from-orange-50 hover:to-yellow-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-orange-50 focus:to-yellow-50"
                onClick={() => column.clearSorting()}
              >
                <X className="text-orange-600" />
                <span className="font-medium">Reset</span>
              </DropdownMenuItem>
            )}
          </>
        )}
        {column.getCanHide() && (
          <DropdownMenuCheckboxItem
            className="relative pr-8 pl-3 py-2.5 hover:bg-gradient-to-r hover:from-red-50 hover:to-pink-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-red-50 focus:to-pink-50 [&>span:first-child]:right-2 [&>span:first-child]:left-auto"
            checked={!column.getIsVisible()}
            onClick={() => column.toggleVisibility(false)}
          >
            <EyeOff className="text-red-600" />
            <span className="font-medium">Hide Column</span>
          </DropdownMenuCheckboxItem>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
