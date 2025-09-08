import * as React from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { Zap, ArrowRight } from "lucide-react";

interface QuickAction {
  id: string;
  title: string;
  description?: string;
  icon: React.ReactNode;
  onClick: () => void;
  badge?: {
    text: string;
    variant?: "default" | "secondary" | "destructive" | "outline";
  };
  variant?: "default" | "primary" | "secondary" | "success" | "warning" | "danger";
  disabled?: boolean;
  isLoading?: boolean;
}

interface QuickActionsProps {
  actions: QuickAction[];
  title?: string;
  columns?: 1 | 2 | 3 | 4;
  isLoading?: boolean;
  className?: string;
}

const actionVariantStyles = {
  default: {
    card: "border-gray-200 hover:border-gray-300 hover:bg-gray-50/50",
    icon: "text-gray-600",
    title: "text-gray-900",
    description: "text-gray-600",
  },
  primary: {
    card: "border-blue-200 hover:border-blue-300 hover:bg-blue-50/50",
    icon: "text-blue-600",
    title: "text-blue-900",
    description: "text-blue-600",
  },
  secondary: {
    card: "border-purple-200 hover:border-purple-300 hover:bg-purple-50/50",
    icon: "text-purple-600",
    title: "text-purple-900",
    description: "text-purple-600",
  },
  success: {
    card: "border-green-200 hover:border-green-300 hover:bg-green-50/50",
    icon: "text-green-600",
    title: "text-green-900",
    description: "text-green-600",
  },
  warning: {
    card: "border-orange-200 hover:border-orange-300 hover:bg-orange-50/50",
    icon: "text-orange-600",
    title: "text-orange-900",
    description: "text-orange-600",
  },
  danger: {
    card: "border-red-200 hover:border-red-300 hover:bg-red-50/50",
    icon: "text-red-600",
    title: "text-red-900",
    description: "text-red-600",
  },
};

export function QuickActions({
  actions,
  title = "Quick Actions",
  columns = 2,
  isLoading = false,
  className = "",
}: QuickActionsProps) {

  const gridClasses = {
    1: "grid-cols-1",
    2: "grid-cols-1 md:grid-cols-2",
    3: "grid-cols-1 md:grid-cols-2 lg:grid-cols-3",
    4: "grid-cols-1 md:grid-cols-2 lg:grid-cols-4",
  };

  if (isLoading) {
    return <QuickActionsSkeleton title={title} columns={columns} />;
  }

  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardHeader className="pb-4">
        <CardTitle className="flex items-center gap-2">
          <Zap className="h-5 w-5" />
          {title}
        </CardTitle>
      </CardHeader>

      <CardContent>
        <div className={cn("grid gap-4", gridClasses[columns])}>
          {actions.map((action) => (
            <QuickActionCard key={action.id} action={action} />
          ))}
        </div>
        
        {actions.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            No quick actions available
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function QuickActionCard({ action }: { action: QuickAction }) {
  const styles = actionVariantStyles[action.variant || 'default'];
  
  return (
    <Card
      className={cn(
        "cursor-pointer transition-all duration-200 hover:shadow-md transform hover:-translate-y-1",
        styles.card,
        action.disabled && "opacity-50 cursor-not-allowed hover:transform-none"
      )}
      onClick={() => !action.disabled && !action.isLoading && action.onClick()}
    >
      <CardContent className="p-4">
        <div className="flex items-start justify-between space-x-3">
          <div className="flex-1 space-y-2">
            <div className="flex items-center gap-2">
              <div className={cn("h-8 w-8 flex items-center justify-center", styles.icon)}>
                {action.isLoading ? (
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                ) : (
                  action.icon
                )}
              </div>
              
              <div className="flex-1">
                <h3 className={cn("font-medium text-sm leading-tight", styles.title)}>
                  {action.title}
                </h3>
                
                {action.badge && (
                  <Badge variant={action.badge.variant || "secondary"} className="text-xs mt-1">
                    {action.badge.text}
                  </Badge>
                )}
              </div>
            </div>
            
            {action.description && (
              <p className={cn("text-xs leading-relaxed", styles.description)}>
                {action.description}
              </p>
            )}
          </div>

          <ArrowRight className={cn("h-4 w-4 opacity-50", styles.icon)} />
        </div>
      </CardContent>
    </Card>
  );
}

// Skeleton for loading states
function QuickActionsSkeleton({ 
  title, 
  columns 
}: { 
  title: string; 
  columns: number;
}) {
  const gridClasses = {
    1: "grid-cols-1",
    2: "grid-cols-1 md:grid-cols-2",
    3: "grid-cols-1 md:grid-cols-2 lg:grid-cols-3",
    4: "grid-cols-1 md:grid-cols-2 lg:grid-cols-4",
  };

  return (
    <Card>
      <CardHeader className="pb-4">
        <div className="flex items-center gap-2">
          <Zap className="h-5 w-5" />
          {title}
        </div>
      </CardHeader>

      <CardContent>
        <div className={cn("grid gap-4", gridClasses[columns])}>
          {Array.from({ length: columns * 2 }).map((_, i) => (
            <Card key={i} className="p-4">
              <div className="flex items-start justify-between space-x-3">
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-8 w-8 rounded" />
                    <Skeleton className="h-4 w-24" />
                  </div>
                  <Skeleton className="h-3 w-32" />
                </div>
                <Skeleton className="h-4 w-4" />
              </div>
            </Card>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

// Predefined quick actions for different roles
export const teacherQuickActions = (navigate: (path: string) => void): QuickAction[] => [
  {
    id: "grade-students",
    title: "Grade Students",
    description: "Enter grades for your classes",
    icon: <span className="text-lg">📝</span>,
    variant: "primary",
    onClick: () => navigate("/teacher/grade-workflow"),
    badge: { text: "New", variant: "default" },
  },
  {
    id: "mark-attendance",
    title: "Mark Attendance",
    description: "Take attendance for today's classes",
    icon: <span className="text-lg">✅</span>,
    variant: "success",
    onClick: () => navigate("/teacher/attendance"),
  },
  {
    id: "view-assignments",
    title: "Teaching Assignments",
    description: "Manage your course assignments",
    icon: <span className="text-lg">📚</span>,
    variant: "secondary",
    onClick: () => navigate("/teacher/teaching-assignments"),
  },
  {
    id: "contact-parents",
    title: "Contact Parents",
    description: "Send messages to parents",
    icon: <span className="text-lg">👨‍👩‍👧‍👦</span>,
    variant: "default",
    onClick: () => navigate("/teacher/parent-contact"),
  },
];

export const studentQuickActions = (navigate: (path: string) => void): QuickAction[] => [
  {
    id: "view-grades",
    title: "My Grades",
    description: "Check your latest grades",
    icon: <span className="text-lg">🎯</span>,
    variant: "primary",
    onClick: () => navigate("/student/grades"),
  },
  {
    id: "assignments",
    title: "Assignments",
    description: "View pending assignments",
    icon: <span className="text-lg">📋</span>,
    variant: "warning",
    onClick: () => navigate("/student/assignments"),
    badge: { text: "3 Due", variant: "destructive" },
  },
  {
    id: "schedule",
    title: "My Schedule",
    description: "View your class schedule",
    icon: <span className="text-lg">📅</span>,
    variant: "secondary",
    onClick: () => navigate("/student/schedule"),
  },
  {
    id: "progress",
    title: "Progress Report",
    description: "Track your academic progress",
    icon: <span className="text-lg">📈</span>,
    variant: "success",
    onClick: () => navigate("/student/progress"),
  },
];

export const parentQuickActions = (navigate: (path: string) => void): QuickAction[] => [
  {
    id: "children-grades",
    title: "Children's Grades",
    description: "View your children's grades",
    icon: <span className="text-lg">📊</span>,
    variant: "primary",
    onClick: () => navigate("/parent/grades"),
  },
  {
    id: "contact-teachers",
    title: "Contact Teachers",
    description: "Message your children's teachers",
    icon: <span className="text-lg">✉️</span>,
    variant: "secondary",
    onClick: () => navigate("/parent/contact"),
  },
  {
    id: "schedule-meeting",
    title: "Schedule Meeting",
    description: "Book parent-teacher meetings",
    icon: <span className="text-lg">🤝</span>,
    variant: "success",
    onClick: () => navigate("/parent/meetings"),
  },
  {
    id: "attendance",
    title: "Attendance",
    description: "Monitor attendance records",
    icon: <span className="text-lg">📋</span>,
    variant: "default",
    onClick: () => navigate("/parent/attendance"),
  },
];

export type { QuickAction, QuickActionsProps };
