import * as React from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: React.ReactNode;
  trend?: {
    value: number;
    isPositive: boolean;
    label?: string;
  };
  variant?: "default" | "blue" | "green" | "purple" | "orange" | "red";
  isLoading?: boolean;
  className?: string;
  onClick?: () => void;
}

const variantStyles = {
  default: {
    card: "border-slate-200/60 bg-gradient-to-br from-slate-50/60 to-gray-50/30 hover:shadow-lg",
    icon: "bg-gradient-to-br from-slate-500 to-slate-600",
    title: "text-slate-900",
    value: "text-slate-700",
    subtitle: "text-slate-600/70",
  },
  blue: {
    card: "border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30 hover:shadow-lg",
    icon: "bg-gradient-to-br from-blue-500 to-blue-600",
    title: "text-blue-900",
    value: "text-blue-700",
    subtitle: "text-blue-600/70",
  },
  green: {
    card: "border-green-200/60 bg-gradient-to-br from-green-50/60 to-emerald-50/30 hover:shadow-lg",
    icon: "bg-gradient-to-br from-green-500 to-green-600",
    title: "text-green-900",
    value: "text-green-700",
    subtitle: "text-green-600/70",
  },
  purple: {
    card: "border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-violet-50/30 hover:shadow-lg",
    icon: "bg-gradient-to-br from-purple-500 to-purple-600",
    title: "text-purple-900",
    value: "text-purple-700",
    subtitle: "text-purple-600/70",
  },
  orange: {
    card: "border-orange-200/60 bg-gradient-to-br from-orange-50/60 to-amber-50/30 hover:shadow-lg",
    icon: "bg-gradient-to-br from-orange-500 to-orange-600",
    title: "text-orange-900",
    value: "text-orange-700",
    subtitle: "text-orange-600/70",
  },
  red: {
    card: "border-red-200/60 bg-gradient-to-br from-red-50/60 to-pink-50/30 hover:shadow-lg",
    icon: "bg-gradient-to-br from-red-500 to-red-600",
    title: "text-red-900",
    value: "text-red-700",
    subtitle: "text-red-600/70",
  },
};

export function StatCard({
  title,
  value,
  subtitle,
  icon,
  trend,
  variant = "default",
  isLoading = false,
  className = "",
  onClick,
}: StatCardProps) {
  const styles = variantStyles[variant];
  
  if (isLoading) {
    return (
      <Card className={cn(styles.card, "transition-shadow duration-300", className)}>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-6 w-6 rounded" />
        </CardHeader>
        <CardContent>
          <Skeleton className="h-8 w-16 mb-2" />
          <Skeleton className="h-3 w-20" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card 
      className={cn(
        styles.card, 
        "transition-all duration-300 transform hover:-translate-y-1",
        onClick && "cursor-pointer",
        className
      )}
      onClick={onClick}
    >
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <h3 className={cn("text-sm font-medium", styles.title)}>
          {title}
        </h3>
        {icon && (
          <div className={cn("h-6 w-6 rounded-lg flex items-center justify-center text-white", styles.icon)}>
            {icon}
          </div>
        )}
      </CardHeader>
      <CardContent>
        <div className={cn("text-2xl font-bold mb-1", styles.value)}>
          {typeof value === 'number' ? value.toLocaleString() : value}
        </div>
        
        <div className="flex items-center justify-between">
          {subtitle && (
            <p className={cn("text-xs", styles.subtitle)}>
              {subtitle}
            </p>
          )}
          
          {trend && (
            <Badge 
              variant={trend.isPositive ? "default" : "destructive"}
              className="text-xs"
            >
              {trend.isPositive ? "+" : ""}{trend.value}%
              {trend.label && ` ${trend.label}`}
            </Badge>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

// Skeleton version for loading states
export function StatCardSkeleton({ className = "" }: { className?: string }) {
  return (
    <Card className={cn("p-6", className)}>
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-6 w-6 rounded" />
        </div>
        <Skeleton className="h-8 w-16" />
        <Skeleton className="h-3 w-24" />
      </div>
    </Card>
  );
}

// Grid component for organizing stat cards
interface StatGridProps {
  children: React.ReactNode;
  columns?: 1 | 2 | 3 | 4 | 5 | 6;
  className?: string;
}

export function StatGrid({ children, columns = 4, className = "" }: StatGridProps) {
  const gridClasses = {
    1: "grid-cols-1",
    2: "grid-cols-1 md:grid-cols-2",
    3: "grid-cols-1 md:grid-cols-2 lg:grid-cols-3",
    4: "grid-cols-1 md:grid-cols-2 lg:grid-cols-4",
    5: "grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5",
    6: "grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6",
  };

  return (
    <div className={cn("grid gap-6", gridClasses[columns], className)}>
      {children}
    </div>
  );
}

export type { StatCardProps };
