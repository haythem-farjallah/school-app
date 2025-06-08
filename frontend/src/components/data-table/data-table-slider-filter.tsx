"use client";

import type { Column } from "@tanstack/react-table";
import * as React from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Separator } from "@/components/ui/separator";
import { Slider } from "@/components/ui/slider";
import { cn } from "@/lib/utils";
import { PlusCircle, XCircle } from "lucide-react";

interface Range {
  min: number;
  max: number;
}

type RangeValue = [number, number];

function getIsValidRange(value: unknown): value is RangeValue {
  return (
    Array.isArray(value) &&
    value.length === 2 &&
    typeof value[0] === "number" &&
    typeof value[1] === "number"
  );
}

interface DataTableSliderFilterProps<TData> {
  column: Column<TData, unknown>;
  title?: string;
}

export function DataTableSliderFilter<TData>({
  column,
  title,
}: DataTableSliderFilterProps<TData>) {
  const id = React.useId();

  const columnFilterValue = getIsValidRange(column.getFilterValue())
    ? (column.getFilterValue() as RangeValue)
    : undefined;

  const defaultRange = column.columnDef.meta?.range;
  const unit = column.columnDef.meta?.unit;

  const { min, max, step } = React.useMemo<Range & { step: number }>(() => {
    let minValue = 0;
    let maxValue = 100;

    if (defaultRange && getIsValidRange(defaultRange)) {
      [minValue, maxValue] = defaultRange;
    } else {
      const values = column.getFacetedMinMaxValues();
      if (values && Array.isArray(values) && values.length === 2) {
        const [facetMinValue, facetMaxValue] = values;
        if (
          typeof facetMinValue === "number" &&
          typeof facetMaxValue === "number"
        ) {
          minValue = facetMinValue;
          maxValue = facetMaxValue;
        }
      }
    }

    const rangeSize = maxValue - minValue;
    const step =
      rangeSize <= 20
        ? 1
        : rangeSize <= 100
          ? Math.ceil(rangeSize / 20)
          : Math.ceil(rangeSize / 50);

    return { min: minValue, max: maxValue, step };
  }, [column, defaultRange]);

  const range = React.useMemo((): RangeValue => {
    return columnFilterValue ?? [min, max];
  }, [columnFilterValue, min, max]);

  const formatValue = React.useCallback((value: number) => {
    return value.toLocaleString(undefined, { maximumFractionDigits: 0 });
  }, []);

  const onFromInputChange = React.useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const numValue = Number(event.target.value);
      if (!Number.isNaN(numValue) && numValue >= min && numValue <= range[1]) {
        column.setFilterValue([numValue, range[1]]);
      }
    },
    [column, min, range],
  );

  const onToInputChange = React.useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const numValue = Number(event.target.value);
      if (!Number.isNaN(numValue) && numValue <= max && numValue >= range[0]) {
        column.setFilterValue([range[0], numValue]);
      }
    },
    [column, max, range],
  );

  const onSliderValueChange = React.useCallback(
    (value: RangeValue) => {
      if (Array.isArray(value) && value.length === 2) {
        column.setFilterValue(value);
      }
    },
    [column],
  );

  const onReset = React.useCallback(
    (event: React.MouseEvent) => {
      if (event.target instanceof HTMLDivElement) {
        event.stopPropagation();
      }
      column.setFilterValue(undefined);
    },
    [column],
  );

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button 
          variant="outline" 
          size="sm" 
          className={cn(
            "border-dashed hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200",
            columnFilterValue && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border-blue-300/60"
          )}
        >
          {columnFilterValue ? (
            <div
              role="button"
              aria-label={`Clear ${title} filter`}
              tabIndex={0}
              className="rounded-sm p-1 hover:bg-red-100/80 transition-all duration-200 hover:scale-110"
              onClick={onReset}
            >
              <XCircle className="h-4 w-4 text-red-600" />
            </div>
          ) : (
            <PlusCircle className="h-4 w-4 text-blue-600" />
          )}
          <span className="font-medium">{title}</span>
          {columnFilterValue ? (
            <>
              <Separator
                orientation="vertical"
                className="mx-2 data-[orientation=vertical]:h-4 bg-blue-300/60"
              />
              <span className="font-mono text-sm bg-blue-50 px-2 py-1 rounded border border-blue-200/60">
                {formatValue(columnFilterValue[0])} - {formatValue(columnFilterValue[1])}
                {unit ? ` ${unit}` : ""}
              </span>
            </>
          ) : null}
        </Button>
      </PopoverTrigger>
      <PopoverContent 
        align="start" 
        className="flex w-auto flex-col gap-6 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl rounded-xl p-6"
      >
        <div className="flex flex-col gap-4">
          <div className="text-center">
            <p className="font-semibold text-lg text-slate-800 mb-1">{title}</p>
            <p className="text-sm text-slate-600">Adjust the range using the slider or input fields</p>
          </div>
          
          <div className="flex items-center gap-3">
            <div className="relative">
              <Label htmlFor={`${id}-from`} className="sr-only">
                From
              </Label>
              <Input
                id={`${id}-from`}
                type="number"
                aria-valuemin={min}
                aria-valuemax={max}
                inputMode="numeric"
                pattern="[0-9]*"
                placeholder={min.toString()}
                min={min}
                max={max}
                value={range[0]?.toString()}
                onChange={onFromInputChange}
                className={cn(
                  "h-10 w-24 text-center font-mono border-slate-300/80 focus:border-blue-400 focus:ring-2 focus:ring-blue-200/60",
                  unit && "pr-8"
                )}
              />
              {unit && (
                <span className="absolute top-0 right-0 bottom-0 flex items-center rounded-r-md bg-gradient-to-r from-blue-50 to-indigo-50 px-2 text-slate-600 text-sm font-medium border-l border-slate-200">
                  {unit}
                </span>
              )}
            </div>
            
            <div className="text-slate-400 font-medium">to</div>
            
            <div className="relative">
              <Label htmlFor={`${id}-to`} className="sr-only">
                to
              </Label>
              <Input
                id={`${id}-to`}
                type="number"
                aria-valuemin={min}
                aria-valuemax={max}
                inputMode="numeric"
                pattern="[0-9]*"
                placeholder={max.toString()}
                min={min}
                max={max}
                value={range[1]?.toString()}
                onChange={onToInputChange}
                className={cn(
                  "h-10 w-24 text-center font-mono border-slate-300/80 focus:border-blue-400 focus:ring-2 focus:ring-blue-200/60",
                  unit && "pr-8"
                )}
              />
              {unit && (
                <span className="absolute top-0 right-0 bottom-0 flex items-center rounded-r-md bg-gradient-to-r from-blue-50 to-indigo-50 px-2 text-slate-600 text-sm font-medium border-l border-slate-200">
                  {unit}
                </span>
              )}
            </div>
          </div>
          
          <div className="px-2 py-4">
            <Label htmlFor={`${id}-slider`} className="sr-only">
              {title} slider
            </Label>
            <Slider
              id={`${id}-slider`}
              min={min}
              max={max}
              step={step}
              value={range}
              onValueChange={onSliderValueChange}
              className="w-full"
            />
            <div className="flex justify-between text-xs text-slate-500 mt-2">
              <span>{formatValue(min)}</span>
              <span>{formatValue(max)}</span>
            </div>
          </div>
        </div>
        
        <Button
          aria-label={`Clear ${title} filter`}
          variant="outline"
          size="sm"
          onClick={onReset}
          className="bg-gradient-to-r from-red-50 to-pink-50 hover:from-red-100 hover:to-pink-100 border-red-200/60 text-red-700 hover:text-red-800 transition-all duration-200"
        >
          <XCircle className="h-4 w-4 mr-2" />
          Clear Filter
        </Button>
      </PopoverContent>
    </Popover>
  );
}
