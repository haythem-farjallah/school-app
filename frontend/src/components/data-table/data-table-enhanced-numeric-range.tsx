"use client";

import * as React from "react";
import type { Column } from "@tanstack/react-table";
import { Hash, X, RotateCcw, TrendingUp, TrendingDown } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

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

interface DataTableEnhancedNumericRangeProps<TData> {
  column: Column<TData, unknown>;
  title?: string;
  unit?: string;
  step?: number;
  precision?: number;
  presets?: Array<{ label: string; value: RangeValue }>;
}

export function DataTableEnhancedNumericRange<TData>({
  column,
  title,
  unit,
  step,
  precision = 0,
  presets = [],
}: DataTableEnhancedNumericRangeProps<TData>) {
  const [open, setOpen] = React.useState(false);
  const [inputValues, setInputValues] = React.useState<{ min: string; max: string }>({ min: "", max: "" });

  const columnFilterValue = getIsValidRange(column.getFilterValue())
    ? (column.getFilterValue() as RangeValue)
    : undefined;

  const defaultRange = column.columnDef.meta?.range;

  const { min, max, calculatedStep } = React.useMemo<Range & { calculatedStep: number }>(() => {
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
    const calculatedStep = step || (
      rangeSize <= 20
        ? 1
        : rangeSize <= 100
          ? Math.ceil(rangeSize / 20)
          : Math.ceil(rangeSize / 50)
    );

    return { min: minValue, max: maxValue, calculatedStep };
  }, [column, defaultRange, step]);

  const range = React.useMemo((): RangeValue => {
    return columnFilterValue ?? [min, max];
  }, [columnFilterValue, min, max]);

  // Update input values when range changes
  React.useEffect(() => {
    setInputValues({
      min: range[0].toString(),
      max: range[1].toString(),
    });
  }, [range]);

  const formatValue = React.useCallback((value: number) => {
    return value.toLocaleString(undefined, { 
      maximumFractionDigits: precision,
      minimumFractionDigits: precision 
    });
  }, [precision]);

  const onMinInputChange = React.useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;
      setInputValues(prev => ({ ...prev, min: value }));
      
      const numValue = Number(value);
      if (!Number.isNaN(numValue) && numValue >= min && numValue <= range[1]) {
        column.setFilterValue([numValue, range[1]]);
      }
    },
    [column, min, range],
  );

  const onMaxInputChange = React.useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;
      setInputValues(prev => ({ ...prev, max: value }));
      
      const numValue = Number(value);
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

  const onReset = React.useCallback(() => {
    column.setFilterValue(undefined);
  }, [column]);

  const onPresetSelect = React.useCallback((presetValue: RangeValue) => {
    column.setFilterValue(presetValue);
  }, [column]);

  const isFiltered = columnFilterValue !== undefined;
  const isFullRange = range[0] === min && range[1] === max;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button 
          variant="outline" 
          size="sm" 
          className={cn(
            "border-dashed hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 shadow-sm hover:shadow-md",
            isFiltered && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border-blue-300/60 shadow-md"
          )}
        >
          <Hash className="h-4 w-4 text-blue-600 mr-2" />
          <span className="font-medium">{title}</span>
          {isFiltered && !isFullRange && (
            <>
              <Separator
                orientation="vertical"
                className="mx-2 data-[orientation=vertical]:h-4 bg-blue-300/60"
              />
              <Badge className="bg-blue-50 text-blue-700 border border-blue-200/60 font-mono text-xs">
                {formatValue(range[0])} - {formatValue(range[1])}
                {unit && ` ${unit}`}
              </Badge>
            </>
          )}
          {isFiltered && (
            <Button
              variant="ghost"
              size="sm"
              onClick={(e) => {
                e.stopPropagation();
                onReset();
              }}
              className="ml-2 h-5 w-5 p-0 hover:bg-red-100/80 hover:text-red-600 transition-all duration-200 hover:scale-110"
            >
              <X className="h-3 w-3" />
            </Button>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent 
        align="start" 
        className="w-[400px] p-0 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl rounded-xl"
      >
        <div className="p-6 space-y-6">
          {/* Header */}
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 mb-2">
              <Hash className="h-5 w-5 text-blue-600" />
              <h3 className="font-semibold text-lg text-slate-800">{title}</h3>
            </div>
            <p className="text-sm text-slate-600">
              Adjust the range using the slider or input fields
            </p>
          </div>

          {/* Current Range Display */}
          <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-4 border border-blue-200/60">
            <div className="flex items-center justify-between">
              <div className="text-center">
                <div className="flex items-center gap-1 text-slate-600 mb-1">
                  <TrendingDown className="h-3 w-3" />
                  <span className="text-xs font-medium">Minimum</span>
                </div>
                <div className="text-lg font-bold text-blue-700">
                  {formatValue(range[0])}{unit && ` ${unit}`}
                </div>
              </div>
              <div className="text-slate-400">to</div>
              <div className="text-center">
                <div className="flex items-center gap-1 text-slate-600 mb-1">
                  <TrendingUp className="h-3 w-3" />
                  <span className="text-xs font-medium">Maximum</span>
                </div>
                <div className="text-lg font-bold text-blue-700">
                  {formatValue(range[1])}{unit && ` ${unit}`}
                </div>
              </div>
            </div>
          </div>

          {/* Presets */}
          {presets.length > 0 && (
            <div className="space-y-2">
              <Label className="text-sm font-medium text-slate-700">Quick Presets</Label>
              <div className="flex flex-wrap gap-2">
                {presets.map((preset, index) => (
                  <Button
                    key={index}
                    variant="outline"
                    size="sm"
                    onClick={() => onPresetSelect(preset.value)}
                    className="text-xs hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 transition-colors duration-200"
                  >
                    {preset.label}
                  </Button>
                ))}
              </div>
            </div>
          )}

          {/* Input Fields */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="min-input" className="text-sm font-medium text-slate-700">
                Minimum Value
              </Label>
              <div className="relative">
                <Input
                  id="min-input"
                  type="number"
                  step={calculatedStep}
                  min={min}
                  max={max}
                  value={inputValues.min}
                  onChange={onMinInputChange}
                  className={cn(
                    "text-center font-mono border-slate-300/80 focus:border-blue-400 focus:ring-2 focus:ring-blue-200/60 transition-all duration-200",
                    unit && "pr-12"
                  )}
                />
                {unit && (
                  <span className="absolute right-3 top-1/2 transform -translate-y-1/2 text-sm text-slate-500 font-medium">
                    {unit}
                  </span>
                )}
              </div>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="max-input" className="text-sm font-medium text-slate-700">
                Maximum Value
              </Label>
              <div className="relative">
                <Input
                  id="max-input"
                  type="number"
                  step={calculatedStep}
                  min={min}
                  max={max}
                  value={inputValues.max}
                  onChange={onMaxInputChange}
                  className={cn(
                    "text-center font-mono border-slate-300/80 focus:border-blue-400 focus:ring-2 focus:ring-blue-200/60 transition-all duration-200",
                    unit && "pr-12"
                  )}
                />
                {unit && (
                  <span className="absolute right-3 top-1/2 transform -translate-y-1/2 text-sm text-slate-500 font-medium">
                    {unit}
                  </span>
                )}
              </div>
            </div>
          </div>
          
          {/* Slider */}
          <div className="space-y-4">
            <Label className="text-sm font-medium text-slate-700">Range Slider</Label>
            <div className="px-3 py-4">
              <Slider
                min={min}
                max={max}
                step={calculatedStep}
                value={range}
                onValueChange={onSliderValueChange}
                className="w-full"
              />
              <div className="flex justify-between text-xs text-slate-500 mt-2">
                <span>{formatValue(min)}{unit && ` ${unit}`}</span>
                <span>{formatValue(max)}{unit && ` ${unit}`}</span>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-between pt-4 border-t border-slate-200/60">
            <div className="text-xs text-slate-500">
              Range: {formatValue(range[1] - range[0])}{unit && ` ${unit}`}
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={onReset}
              className="bg-gradient-to-r from-red-50 to-pink-50 hover:from-red-100 hover:to-pink-100 border-red-200/60 text-red-700 hover:text-red-800 transition-all duration-200"
            >
              <RotateCcw className="h-4 w-4 mr-2" />
              Reset Range
            </Button>
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
