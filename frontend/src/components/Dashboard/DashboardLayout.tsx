import * as React from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

interface DashboardLayoutProps {
  children: React.ReactNode;
  title: string;
  subtitle?: string;
  headerIcon?: React.ReactNode;
  headerActions?: React.ReactNode;
  isLoading?: boolean;
  error?: string | null;
  className?: string;
}

export function DashboardLayout({
  children,
  title,
  subtitle,
  headerIcon,
  headerActions,
  isLoading = false,
  error = null,
  className = "",
}: DashboardLayoutProps) {
  
  if (error) {
    return (
      <div className="space-y-6">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <div className="text-center text-red-600">
              <h3 className="text-lg font-semibold mb-2">Failed to load dashboard</h3>
              <p className="text-sm">{error}</p>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Dashboard Header */}
      <Card className="border-slate-200/60 bg-gradient-to-br from-slate-50/80 via-blue-50/40 to-indigo-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              {headerIcon && (
                <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center shadow-lg">
                  {headerIcon}
                </div>
              )}
              <div className="space-y-2">
                {isLoading ? (
                  <Skeleton className="h-8 w-64" />
                ) : (
                  <CardTitle className="text-3xl font-bold bg-gradient-to-r from-slate-900 via-blue-800 to-indigo-700 bg-clip-text text-transparent">
                    {title}
                  </CardTitle>
                )}
                {subtitle && (
                  isLoading ? (
                    <Skeleton className="h-4 w-48" />
                  ) : (
                    <CardDescription className="text-slate-700/80 text-lg">
                      {subtitle}
                    </CardDescription>
                  )
                )}
              </div>
            </div>
            
            {headerActions && !isLoading && (
              <div className="flex items-center gap-3">
                {headerActions}
              </div>
            )}
          </div>
        </CardHeader>
      </Card>

      {/* Dashboard Content */}
      <div className="space-y-6">
        {isLoading ? (
          <DashboardSkeleton />
        ) : (
          children
        )}
      </div>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      {/* Stats Cards Skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {Array.from({ length: 4 }).map((_, i) => (
          <Card key={i} className="p-6">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-6 w-6 rounded" />
              </div>
              <Skeleton className="h-8 w-16" />
              <Skeleton className="h-3 w-24" />
            </div>
          </Card>
        ))}
      </div>

      {/* Main Content Skeleton */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="p-6">
          <div className="space-y-4">
            <Skeleton className="h-6 w-32" />
            <div className="space-y-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="flex items-center space-x-3">
                  <Skeleton className="h-10 w-10 rounded" />
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-3 w-16" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="space-y-4">
            <Skeleton className="h-6 w-32" />
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="p-3 border rounded-lg">
                  <div className="flex items-center justify-between">
                    <Skeleton className="h-4 w-32" />
                    <Skeleton className="h-4 w-16" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

// Export types for use in other components
export type { DashboardLayoutProps };
