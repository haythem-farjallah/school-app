"use client";

import * as React from "react";
import type { Table } from "@tanstack/react-table";
import { X, Filter, Calendar, Hash, Type, List } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

interface DataTableEnhancedFilterListProps<TData> {
  table: Table<TData>;
  className?: string;
}

export function DataTableEnhancedFilterList<TData>({
  table,
  className,
}: DataTableEnhancedFilterListProps<TData>) {
  const columnFilters = table.getState().columnFilters;
  
  if (columnFilters.length === 0) {
    return null;
  }

  const getFilterIcon = (columnId: string) => {
    const column = table.getColumn(columnId);
    const variant = column?.columnDef.meta?.variant;
    
    switch (variant) {
      case "date":
      case "dateRange":
        return Calendar;
      case "number":
      case "range":
        return Hash;
      case "select":
      case "multiSelect":
        return List;
      default:
        return Type;
    }
  };

  const getFilterLabel = (columnId: string) => {
    const column = table.getColumn(columnId);
    return column?.columnDef.meta?.label || columnId;
  };

  const getFilterValue = (columnId: string, value: any) => {
    const column = table.getColumn(columnId);
    const variant = column?.columnDef.meta?.variant;
    
    if (!value) return "";
    
    if (Array.isArray(value)) {
      if (variant === "range" && value.length === 2) {
        const unit = column?.columnDef.meta?.unit || "";
        return `${value[0]} - ${value[1]}${unit ? ` ${unit}` : ""}`;
      }
      return value.length > 3 
        ? `${value.slice(0, 3).join(", ")} +${value.length - 3} more`
        : value.join(", ");
    }
    
    if (variant === "date" || variant === "dateRange") {
      try {
        const date = new Date(value);
        return date.toLocaleDateString();
      } catch {
        return String(value);
      }
    }
    
    return String(value);
  };

  const clearFilter = (columnId: string) => {
    table.getColumn(columnId)?.setFilterValue(undefined);
  };

  const clearAllFilters = () => {
    table.resetColumnFilters();
  };

  return (
    <div className={cn("space-y-3", className)}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-slate-500" />
          <span className="text-sm font-medium text-slate-700">
            Active Filters ({columnFilters.length})
          </span>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={clearAllFilters}
          className="text-xs text-slate-500 hover:text-red-600 hover:bg-red-50 transition-colors duration-200"
        >
          <X className="h-3 w-3 mr-1" />
          Clear All
        </Button>
      </div>

      {/* Filter Tags */}
      <div className="flex flex-wrap gap-2">
        <AnimatePresence>
          {columnFilters.map((filter) => {
            const Icon = getFilterIcon(filter.id);
            const label = getFilterLabel(filter.id);
            const displayValue = getFilterValue(filter.id, filter.value);
            
            return (
              <motion.div
                key={filter.id}
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                transition={{ duration: 0.2 }}
              >
                <Badge
                  variant="outline"
                  className="group relative pl-2 pr-8 py-1.5 bg-gradient-to-r from-blue-50 to-indigo-50 border-blue-200/60 text-blue-800 hover:from-blue-100 hover:to-indigo-100 transition-all duration-200 shadow-sm hover:shadow-md"
                >
                  <div className="flex items-center gap-2">
                    <Icon className="h-3 w-3" />
                    <span className="font-medium text-xs">{label}:</span>
                    <span className="text-xs max-w-[120px] truncate" title={displayValue}>
                      {displayValue}
                    </span>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => clearFilter(filter.id)}
                    className="absolute right-0 top-0 h-full w-6 p-0 hover:bg-red-100/80 hover:text-red-600 transition-colors duration-200 rounded-l-none"
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </Badge>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>

      {/* Filter Summary */}
      {columnFilters.length > 0 && (
        <div className="flex items-center gap-2 text-xs text-slate-500 pt-2 border-t border-slate-200/60">
          <span>
            Showing filtered results • {table.getFilteredRowModel().rows.length} items match your criteria
          </span>
        </div>
      )}
    </div>
  );
}
