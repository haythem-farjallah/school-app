"use client";

import * as React from "react";
import type { Table } from "@tanstack/react-table";
import { Filter, X, RotateCcw, Settings, ChevronDown } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DataTableFilterMenu } from "./data-table-filter-menu";
import { FilterPresets } from "./filter-presets";
import { cn } from "@/lib/utils";

interface DataTableAdvancedFilterBarProps<TData> {
  table: Table<TData>;
  entityType?: string;
  className?: string;
}

export function DataTableAdvancedFilterBar<TData>({
  table,
  entityType = "items",
  className,
}: DataTableAdvancedFilterBarProps<TData>) {
  const [isExpanded, setIsExpanded] = React.useState(false);
  
  // Get active filters count
  const activeFiltersCount = table.getState().columnFilters.length;
  const hasActiveFilters = activeFiltersCount > 0;
  
  // Get filtered row count
  const totalRows = table.getFilteredRowModel().rows.length;
  const selectedRows = table.getFilteredSelectedRowModel().rows.length;

  const clearAllFilters = React.useCallback(() => {
    table.resetColumnFilters();
  }, [table]);

  const toggleExpanded = React.useCallback(() => {
    setIsExpanded(!isExpanded);
  }, [isExpanded]);

  return (
    <div className={cn("space-y-4", className)}>
      {/* Main Filter Bar */}
      <div className="flex items-center justify-between p-4 bg-gradient-to-r from-slate-50/80 via-blue-50/40 to-indigo-50/20 rounded-xl border border-slate-200/60 shadow-sm">
        <div className="flex items-center gap-4">
          {/* Filter Toggle */}
          <Button
            variant="outline"
            size="sm"
            onClick={toggleExpanded}
            className={cn(
              "transition-all duration-200 shadow-sm hover:shadow-md",
              isExpanded 
                ? "bg-gradient-to-r from-blue-100 to-indigo-100 border-blue-300 text-blue-700" 
                : "hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50"
            )}
          >
            <Filter className="h-4 w-4 mr-2" />
            Filters
            <ChevronDown className={cn(
              "h-4 w-4 ml-2 transition-transform duration-200",
              isExpanded && "rotate-180"
            )} />
            {hasActiveFilters && (
              <Badge className="ml-2 bg-blue-500 text-white">
                {activeFiltersCount}
              </Badge>
            )}
          </Button>

          {/* Quick Actions */}
          <div className="flex items-center gap-2">
            <FilterPresets table={table} entityType={entityType} />
            <DataTableFilterMenu table={table} />
          </div>

          {/* Active Filters Summary */}
          {hasActiveFilters && (
            <motion.div
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              className="flex items-center gap-2"
            >
              <Separator orientation="vertical" className="h-6" />
              <div className="flex items-center gap-2 text-sm text-slate-600">
                <span className="font-medium">{activeFiltersCount} active filter{activeFiltersCount !== 1 ? 's' : ''}</span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={clearAllFilters}
                  className="h-6 px-2 text-xs hover:bg-red-50 hover:text-red-600 transition-colors duration-200"
                >
                  <X className="h-3 w-3 mr-1" />
                  Clear all
                </Button>
              </div>
            </motion.div>
          )}
        </div>

        {/* Results Summary */}
        <div className="flex items-center gap-4 text-sm text-slate-600">
          {selectedRows > 0 && (
            <div className="flex items-center gap-2">
              <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                {selectedRows} selected
              </Badge>
            </div>
          )}
          <div className="font-medium">
            {totalRows.toLocaleString()} {entityType}
            {hasActiveFilters && (
              <span className="text-slate-400 ml-1">filtered</span>
            )}
          </div>
        </div>
      </div>

      {/* Expanded Filter Controls */}
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="p-4 bg-white/95 backdrop-blur-sm rounded-xl border border-slate-200/60 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Settings className="h-4 w-4 text-slate-500" />
                  <h3 className="font-semibold text-slate-800">Advanced Filters</h3>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={clearAllFilters}
                  disabled={!hasActiveFilters}
                  className="text-slate-500 hover:text-red-600 hover:bg-red-50 transition-colors duration-200"
                >
                  <RotateCcw className="h-4 w-4 mr-2" />
                  Reset All
                </Button>
              </div>
              
              {/* Filter Controls Container */}
              <div className="space-y-4">
                <div className="text-sm text-slate-600 bg-slate-50/80 rounded-lg p-3 border border-slate-200/60">
                  <p className="font-medium mb-1">Filter Tips:</p>
                  <ul className="space-y-1 text-xs">
                    <li>• Use the search boxes to find specific values</li>
                    <li>• Multiple selections work with OR logic by default</li>
                    <li>• Date ranges help narrow down time-based data</li>
                    <li>• Numeric ranges are great for age, scores, or quantities</li>
                  </ul>
                </div>
                
                {/* This is where individual filter components will be rendered by the parent */}
                <div className="min-h-[60px] flex items-center justify-center text-slate-400 text-sm border-2 border-dashed border-slate-200 rounded-lg">
                  Filter controls will appear here when implemented
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
