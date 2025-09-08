import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { 
  CheckCircle, 
  XCircle, 
  Clock, 
  AlertCircle, 
  Heart, 
  Stethoscope,
  HelpCircle 
} from "lucide-react";
import { AttendanceStatus } from "@/types/attendance";
import { cn } from "@/lib/utils";

interface AttendanceStatusBadgeProps {
  status: AttendanceStatus;
  size?: "sm" | "md" | "lg";
  interactive?: boolean;
  onClick?: () => void;
  className?: string;
}

const statusConfig = {
  [AttendanceStatus.PRESENT]: {
    label: "Present",
    icon: CheckCircle,
    className: "bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 border-green-200/60 hover:from-green-200 hover:to-emerald-200",
    color: "text-green-600",
  },
  [AttendanceStatus.ABSENT]: {
    label: "Absent",
    icon: XCircle,
    className: "bg-gradient-to-r from-red-100 to-rose-100 text-red-800 border-red-200/60 hover:from-red-200 hover:to-rose-200",
    color: "text-red-600",
  },
  [AttendanceStatus.LATE]: {
    label: "Late",
    icon: Clock,
    className: "bg-gradient-to-r from-yellow-100 to-amber-100 text-yellow-800 border-yellow-200/60 hover:from-yellow-200 hover:to-amber-200",
    color: "text-yellow-600",
  },
  [AttendanceStatus.EXCUSED]: {
    label: "Excused",
    icon: AlertCircle,
    className: "bg-gradient-to-r from-orange-100 to-yellow-100 text-orange-800 border-orange-200/60 hover:from-orange-200 hover:to-yellow-200",
    color: "text-orange-600",
  },
  [AttendanceStatus.SICK_LEAVE]: {
    label: "Sick Leave",
    icon: Heart,
    className: "bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 border-purple-200/60 hover:from-purple-200 hover:to-pink-200",
    color: "text-purple-600",
  },
  [AttendanceStatus.MEDICAL_LEAVE]: {
    label: "Medical Leave",
    icon: Stethoscope,
    className: "bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-800 border-blue-200/60 hover:from-blue-200 hover:to-indigo-200",
    color: "text-blue-600",
  },
  [AttendanceStatus.OTHER]: {
    label: "Other",
    icon: HelpCircle,
    className: "bg-gradient-to-r from-gray-100 to-slate-100 text-gray-800 border-gray-200/60 hover:from-gray-200 hover:to-slate-200",
    color: "text-gray-600",
  },
};

const sizeConfig = {
  sm: {
    badge: "text-xs px-2 py-1",
    icon: "h-3 w-3",
  },
  md: {
    badge: "text-sm px-3 py-1.5",
    icon: "h-4 w-4",
  },
  lg: {
    badge: "text-base px-4 py-2",
    icon: "h-5 w-5",
  },
};

export function AttendanceStatusBadge({
  status,
  size = "md",
  interactive = false,
  onClick,
  className,
}: AttendanceStatusBadgeProps) {
  const config = statusConfig[status];
  const sizeStyles = sizeConfig[size];
  const IconComponent = config.icon;

  if (interactive && onClick) {
    return (
      <Button
        variant="outline"
        size="sm"
        onClick={onClick}
        className={cn(
          "font-semibold transition-all duration-200",
          config.className,
          sizeStyles.badge,
          "hover:scale-105 active:scale-95",
          className
        )}
      >
        <IconComponent className={cn(sizeStyles.icon, "mr-1.5")} />
        {config.label}
      </Button>
    );
  }

  return (
    <Badge
      variant="outline"
      className={cn(
        "font-semibold transition-colors duration-200",
        config.className,
        sizeStyles.badge,
        className
      )}
    >
      <IconComponent className={cn(sizeStyles.icon, "mr-1.5")} />
      {config.label}
    </Badge>
  );
}

// Quick status selector component
interface AttendanceStatusSelectorProps {
  currentStatus?: AttendanceStatus;
  onStatusChange: (status: AttendanceStatus) => void;
  disabled?: boolean;
  size?: "sm" | "md" | "lg";
  showAllStatuses?: boolean;
}

export function AttendanceStatusSelector({
  currentStatus,
  onStatusChange,
  disabled = false,
  size = "sm",
  showAllStatuses = false,
}: AttendanceStatusSelectorProps) {
  const commonStatuses = [
    AttendanceStatus.PRESENT,
    AttendanceStatus.ABSENT,
    AttendanceStatus.LATE,
    AttendanceStatus.EXCUSED,
  ];

  const allStatuses = [
    ...commonStatuses,
    AttendanceStatus.SICK_LEAVE,
    AttendanceStatus.MEDICAL_LEAVE,
    AttendanceStatus.OTHER,
  ];

  const statusesToShow = showAllStatuses ? allStatuses : commonStatuses;

  return (
    <div className="flex flex-wrap gap-1">
      {statusesToShow.map((status) => (
        <AttendanceStatusBadge
          key={status}
          status={status}
          size={size}
          interactive
          onClick={() => !disabled && onStatusChange(status)}
          className={cn(
            currentStatus === status && "ring-2 ring-offset-1 ring-blue-500",
            disabled && "opacity-50 cursor-not-allowed"
          )}
        />
      ))}
    </div>
  );
}

// Compact status indicator for calendar views
interface AttendanceStatusDotProps {
  status: AttendanceStatus;
  size?: "sm" | "md" | "lg";
  className?: string;
}

export function AttendanceStatusDot({ 
  status, 
  size = "md", 
  className 
}: AttendanceStatusDotProps) {
  const config = statusConfig[status];
  
  const sizeClasses = {
    sm: "h-2 w-2",
    md: "h-3 w-3", 
    lg: "h-4 w-4",
  };

  return (
    <div
      className={cn(
        "rounded-full border-2",
        sizeClasses[size],
        config.className.includes("green") && "bg-green-500 border-green-600",
        config.className.includes("red") && "bg-red-500 border-red-600",
        config.className.includes("yellow") && "bg-yellow-500 border-yellow-600",
        config.className.includes("orange") && "bg-orange-500 border-orange-600",
        config.className.includes("purple") && "bg-purple-500 border-purple-600",
        config.className.includes("blue") && "bg-blue-500 border-blue-600",
        config.className.includes("gray") && "bg-gray-500 border-gray-600",
        className
      )}
      title={config.label}
    />
  );
}
