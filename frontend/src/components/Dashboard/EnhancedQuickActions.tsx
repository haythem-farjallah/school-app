"use client";

import * as React from "react";
import { motion } from "framer-motion";
import { 
  Plus, 
  FileText, 
  Users, 
  Calendar, 
  BookOpen, 
  MessageSquare, 
  Settings, 
  Download,
  Upload,
  Eye,
  Edit,
  BarChart3,
  Clock,
  Award,
  Bell,
  Search,
  Filter,
  ArrowRight
} from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export interface QuickAction {
  id: string;
  title: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  onClick: () => void;
  variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger';
  badge?: {
    text: string;
    variant?: 'default' | 'secondary' | 'destructive' | 'outline';
  };
  disabled?: boolean;
  shortcut?: string;
}

interface EnhancedQuickActionsProps {
  title?: string;
  actions: QuickAction[];
  columns?: 2 | 3 | 4;
  className?: string;
  showShortcuts?: boolean;
}

const variantConfig = {
  primary: {
    card: "border-blue-200/60 bg-gradient-to-br from-blue-50/80 to-indigo-50/40 hover:from-blue-100/80 hover:to-indigo-100/60",
    icon: "text-blue-600",
    title: "text-blue-900",
    description: "text-blue-700/80"
  },
  secondary: {
    card: "border-slate-200/60 bg-gradient-to-br from-slate-50/80 to-gray-50/40 hover:from-slate-100/80 hover:to-gray-100/60",
    icon: "text-slate-600",
    title: "text-slate-900",
    description: "text-slate-700/80"
  },
  success: {
    card: "border-green-200/60 bg-gradient-to-br from-green-50/80 to-emerald-50/40 hover:from-green-100/80 hover:to-emerald-100/60",
    icon: "text-green-600",
    title: "text-green-900",
    description: "text-green-700/80"
  },
  warning: {
    card: "border-orange-200/60 bg-gradient-to-br from-orange-50/80 to-amber-50/40 hover:from-orange-100/80 hover:to-amber-100/60",
    icon: "text-orange-600",
    title: "text-orange-900",
    description: "text-orange-700/80"
  },
  danger: {
    card: "border-red-200/60 bg-gradient-to-br from-red-50/80 to-pink-50/40 hover:from-red-100/80 hover:to-pink-100/60",
    icon: "text-red-600",
    title: "text-red-900",
    description: "text-red-700/80"
  }
};

function QuickActionCard({ 
  action, 
  showShortcuts = false 
}: { 
  action: QuickAction; 
  showShortcuts?: boolean;
}) {
  const variant = action.variant || 'secondary';
  const config = variantConfig[variant];
  const Icon = action.icon;

  return (
    <motion.div
      whileHover={{ scale: 1.02, y: -2 }}
      whileTap={{ scale: 0.98 }}
      transition={{ duration: 0.2 }}
    >
      <Button
        variant="ghost"
        onClick={action.onClick}
        disabled={action.disabled}
        className={cn(
          "h-auto p-0 w-full transition-all duration-300 shadow-sm hover:shadow-lg",
          action.disabled && "opacity-50 cursor-not-allowed"
        )}
      >
        <Card className={cn(
          "w-full border transition-all duration-300 transform",
          config.card,
          action.disabled && "opacity-50"
        )}>
          <CardContent className="p-4">
            <div className="flex items-start justify-between mb-3">
              <div className={cn(
                "w-12 h-12 rounded-xl flex items-center justify-center",
                "bg-white/80 shadow-sm border border-white/40"
              )}>
                <Icon className={cn("h-6 w-6", config.icon)} />
              </div>
              
              <div className="flex flex-col items-end gap-1">
                {action.badge && (
                  <Badge 
                    variant={action.badge.variant || 'default'}
                    className="text-xs"
                  >
                    {action.badge.text}
                  </Badge>
                )}
                {showShortcuts && action.shortcut && (
                  <Badge variant="outline" className="text-xs bg-white/60">
                    {action.shortcut}
                  </Badge>
                )}
              </div>
            </div>
            
            <div className="text-left">
              <h3 className={cn("font-semibold mb-1", config.title)}>
                {action.title}
              </h3>
              <p className={cn("text-sm leading-relaxed", config.description)}>
                {action.description}
              </p>
            </div>
            
            <div className="flex items-center justify-end mt-3">
              <ArrowRight className={cn("h-4 w-4", config.icon)} />
            </div>
          </CardContent>
        </Card>
      </Button>
    </motion.div>
  );
}

export function EnhancedQuickActions({
  title = "Quick Actions",
  actions,
  columns = 3,
  className,
  showShortcuts = false
}: EnhancedQuickActionsProps) {
  const gridClasses = {
    2: "grid-cols-1 md:grid-cols-2",
    3: "grid-cols-1 md:grid-cols-2 lg:grid-cols-3",
    4: "grid-cols-1 md:grid-cols-2 lg:grid-cols-4"
  };

  return (
    <Card className={cn(
      "bg-white/95 backdrop-blur-sm shadow-xl border-slate-200/60",
      className
    )}>
      <CardHeader className="pb-4">
        <CardTitle className="text-xl font-bold bg-gradient-to-r from-slate-900 to-blue-800 bg-clip-text text-transparent">
          {title}
        </CardTitle>
        <p className="text-sm text-slate-600">
          Quick access to frequently used features
        </p>
      </CardHeader>
      
      <CardContent>
        {actions.length === 0 ? (
          <div className="text-center py-8">
            <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Settings className="h-8 w-8 text-slate-400" />
            </div>
            <h3 className="font-medium text-slate-900 mb-2">No actions available</h3>
            <p className="text-sm text-slate-500">
              Quick actions will appear here when configured.
            </p>
          </div>
        ) : (
          <div className={cn("grid gap-4", gridClasses[columns])}>
            {actions.map((action, index) => (
              <motion.div
                key={action.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, delay: index * 0.1 }}
              >
                <QuickActionCard 
                  action={action} 
                  showShortcuts={showShortcuts}
                />
              </motion.div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

// Predefined action sets for different roles

export const teacherQuickActions: QuickAction[] = [
  {
    id: "create-assignment",
    title: "Create Assignment",
    description: "Create and assign new tasks to your students",
    icon: Plus,
    onClick: () => console.log("Create assignment"),
    variant: "primary",
    shortcut: "Ctrl+N"
  },
  {
    id: "grade-submissions",
    title: "Grade Submissions",
    description: "Review and grade pending student submissions",
    icon: Edit,
    onClick: () => console.log("Grade submissions"),
    variant: "warning",
    badge: { text: "12 pending", variant: "destructive" },
    shortcut: "Ctrl+G"
  },
  {
    id: "view-classes",
    title: "My Classes",
    description: "View and manage your assigned classes",
    icon: Users,
    onClick: () => console.log("View classes"),
    variant: "secondary",
    shortcut: "Ctrl+C"
  },
  {
    id: "attendance",
    title: "Take Attendance",
    description: "Mark student attendance for today's classes",
    icon: Clock,
    onClick: () => console.log("Take attendance"),
    variant: "success",
    shortcut: "Ctrl+A"
  },
  {
    id: "schedule",
    title: "Class Schedule",
    description: "View your teaching schedule and upcoming classes",
    icon: Calendar,
    onClick: () => console.log("View schedule"),
    variant: "secondary",
    shortcut: "Ctrl+S"
  },
  {
    id: "reports",
    title: "Generate Reports",
    description: "Create performance and progress reports",
    icon: BarChart3,
    onClick: () => console.log("Generate reports"),
    variant: "secondary",
    shortcut: "Ctrl+R"
  }
];

export const studentQuickActions: QuickAction[] = [
  {
    id: "view-assignments",
    title: "My Assignments",
    description: "View and submit your pending assignments",
    icon: FileText,
    onClick: () => console.log("View assignments"),
    variant: "primary",
    badge: { text: "3 due soon", variant: "destructive" },
    shortcut: "Ctrl+A"
  },
  {
    id: "view-grades",
    title: "My Grades",
    description: "Check your latest grades and academic progress",
    icon: Award,
    onClick: () => console.log("View grades"),
    variant: "success",
    shortcut: "Ctrl+G"
  },
  {
    id: "course-materials",
    title: "Course Materials",
    description: "Access learning resources and course content",
    icon: BookOpen,
    onClick: () => console.log("Course materials"),
    variant: "secondary",
    shortcut: "Ctrl+M"
  },
  {
    id: "schedule",
    title: "Class Schedule",
    description: "View your class timetable and upcoming sessions",
    icon: Calendar,
    onClick: () => console.log("View schedule"),
    variant: "secondary",
    shortcut: "Ctrl+S"
  },
  {
    id: "messages",
    title: "Messages",
    description: "Check messages from teachers and classmates",
    icon: MessageSquare,
    onClick: () => console.log("View messages"),
    variant: "secondary",
    badge: { text: "2 new", variant: "default" },
    shortcut: "Ctrl+I"
  },
  {
    id: "profile",
    title: "My Profile",
    description: "Update your profile and account settings",
    icon: Settings,
    onClick: () => console.log("View profile"),
    variant: "secondary",
    shortcut: "Ctrl+P"
  }
];

export const parentQuickActions: QuickAction[] = [
  {
    id: "children-overview",
    title: "Children Overview",
    description: "View academic progress of all your children",
    icon: Users,
    onClick: () => console.log("Children overview"),
    variant: "primary",
    shortcut: "Ctrl+C"
  },
  {
    id: "grades-reports",
    title: "Grades & Reports",
    description: "Check latest grades and progress reports",
    icon: BarChart3,
    onClick: () => console.log("Grades reports"),
    variant: "success",
    shortcut: "Ctrl+G"
  },
  {
    id: "attendance-record",
    title: "Attendance Record",
    description: "Monitor your children's attendance patterns",
    icon: Clock,
    onClick: () => console.log("Attendance record"),
    variant: "secondary",
    shortcut: "Ctrl+A"
  },
  {
    id: "teacher-communication",
    title: "Teacher Messages",
    description: "Communicate with your children's teachers",
    icon: MessageSquare,
    onClick: () => console.log("Teacher messages"),
    variant: "secondary",
    badge: { text: "1 new", variant: "default" },
    shortcut: "Ctrl+T"
  },
  {
    id: "school-events",
    title: "School Events",
    description: "View upcoming school events and activities",
    icon: Calendar,
    onClick: () => console.log("School events"),
    variant: "secondary",
    shortcut: "Ctrl+E"
  },
  {
    id: "notifications",
    title: "Notifications",
    description: "Manage notification preferences and alerts",
    icon: Bell,
    onClick: () => console.log("Notifications"),
    variant: "secondary",
    shortcut: "Ctrl+N"
  }
];

export const adminQuickActions: QuickAction[] = [
  {
    id: "user-management",
    title: "User Management",
    description: "Manage students, teachers, and staff accounts",
    icon: Users,
    onClick: () => console.log("User management"),
    variant: "primary",
    shortcut: "Ctrl+U"
  },
  {
    id: "system-reports",
    title: "System Reports",
    description: "Generate comprehensive system and academic reports",
    icon: BarChart3,
    onClick: () => console.log("System reports"),
    variant: "secondary",
    shortcut: "Ctrl+R"
  },
  {
    id: "bulk-operations",
    title: "Bulk Operations",
    description: "Perform bulk imports, exports, and updates",
    icon: Upload,
    onClick: () => console.log("Bulk operations"),
    variant: "warning",
    shortcut: "Ctrl+B"
  },
  {
    id: "system-settings",
    title: "System Settings",
    description: "Configure system preferences and policies",
    icon: Settings,
    onClick: () => console.log("System settings"),
    variant: "secondary",
    shortcut: "Ctrl+S"
  },
  {
    id: "data-analytics",
    title: "Data Analytics",
    description: "View detailed analytics and performance metrics",
    icon: Search,
    onClick: () => console.log("Data analytics"),
    variant: "success",
    shortcut: "Ctrl+D"
  },
  {
    id: "backup-restore",
    title: "Backup & Restore",
    description: "Manage system backups and data recovery",
    icon: Download,
    onClick: () => console.log("Backup restore"),
    variant: "danger",
    shortcut: "Ctrl+K"
  }
];
