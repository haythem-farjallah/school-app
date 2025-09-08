"use client";

import * as React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Clock, 
  User, 
  BookOpen, 
  FileText, 
  Award, 
  AlertCircle, 
  CheckCircle, 
  Calendar,
  MessageSquare,
  TrendingUp,
  Eye,
  ExternalLink
} from "lucide-react";
import { formatDistanceToNow } from "date-fns";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

export interface ActivityItem {
  id: string;
  type: 'grade' | 'assignment' | 'attendance' | 'announcement' | 'achievement' | 'message' | 'system' | 'event';
  title: string;
  description: string;
  timestamp: Date;
  actionUrl?: string;
  metadata?: {
    grade?: number;
    subject?: string;
    className?: string;
    teacherName?: string;
    priority?: 'low' | 'medium' | 'high';
    status?: 'completed' | 'pending' | 'overdue';
  };
}

interface EnhancedActivityFeedProps {
  activities: ActivityItem[];
  title?: string;
  maxItems?: number;
  showViewAll?: boolean;
  onViewAll?: () => void;
  onItemClick?: (activity: ActivityItem) => void;
  className?: string;
  isLoading?: boolean;
}

const activityConfig = {
  grade: {
    icon: Award,
    color: "text-green-600",
    bgColor: "bg-green-100",
    borderColor: "border-green-200",
    label: "Grade"
  },
  assignment: {
    icon: FileText,
    color: "text-blue-600",
    bgColor: "bg-blue-100",
    borderColor: "border-blue-200",
    label: "Assignment"
  },
  attendance: {
    icon: CheckCircle,
    color: "text-purple-600",
    bgColor: "bg-purple-100",
    borderColor: "border-purple-200",
    label: "Attendance"
  },
  announcement: {
    icon: MessageSquare,
    color: "text-orange-600",
    bgColor: "bg-orange-100",
    borderColor: "border-orange-200",
    label: "Announcement"
  },
  achievement: {
    icon: TrendingUp,
    color: "text-yellow-600",
    bgColor: "bg-yellow-100",
    borderColor: "border-yellow-200",
    label: "Achievement"
  },
  message: {
    icon: MessageSquare,
    color: "text-indigo-600",
    bgColor: "bg-indigo-100",
    borderColor: "border-indigo-200",
    label: "Message"
  },
  system: {
    icon: AlertCircle,
    color: "text-gray-600",
    bgColor: "bg-gray-100",
    borderColor: "border-gray-200",
    label: "System"
  },
  event: {
    icon: Calendar,
    color: "text-pink-600",
    bgColor: "bg-pink-100",
    borderColor: "border-pink-200",
    label: "Event"
  }
};

function ActivityItemComponent({ 
  activity, 
  onClick 
}: { 
  activity: ActivityItem; 
  onClick?: (activity: ActivityItem) => void;
}) {
  const config = activityConfig[activity.type];
  const Icon = config.icon;
  
  const getPriorityColor = (priority?: string) => {
    switch (priority) {
      case 'high': return 'text-red-600 bg-red-50 border-red-200';
      case 'medium': return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'low': return 'text-green-600 bg-green-50 border-green-200';
      default: return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'completed': return 'text-green-700 bg-green-100';
      case 'pending': return 'text-yellow-700 bg-yellow-100';
      case 'overdue': return 'text-red-700 bg-red-100';
      default: return 'text-gray-700 bg-gray-100';
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      transition={{ duration: 0.2 }}
      className={cn(
        "group relative p-4 rounded-lg border transition-all duration-200",
        "hover:shadow-md hover:bg-gradient-to-r hover:from-slate-50 hover:to-blue-50/30",
        onClick && "cursor-pointer",
        config.borderColor
      )}
      onClick={() => onClick?.(activity)}
    >
      <div className="flex items-start gap-3">
        {/* Icon */}
        <div className={cn(
          "flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center",
          config.bgColor
        )}>
          <Icon className={cn("h-5 w-5", config.color)} />
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div className="flex-1 min-w-0">
              <h4 className="font-semibold text-slate-900 truncate group-hover:text-blue-900 transition-colors">
                {activity.title}
              </h4>
              <p className="text-sm text-slate-600 mt-1 line-clamp-2">
                {activity.description}
              </p>
            </div>
            
            {/* Metadata badges */}
            <div className="flex flex-col items-end gap-1 flex-shrink-0">
              {activity.metadata?.priority && (
                <Badge 
                  variant="outline" 
                  className={cn("text-xs", getPriorityColor(activity.metadata.priority))}
                >
                  {activity.metadata.priority}
                </Badge>
              )}
              {activity.metadata?.status && (
                <Badge 
                  variant="outline" 
                  className={cn("text-xs", getStatusColor(activity.metadata.status))}
                >
                  {activity.metadata.status}
                </Badge>
              )}
            </div>
          </div>

          {/* Additional metadata */}
          <div className="flex items-center gap-4 mt-2 text-xs text-slate-500">
            <div className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              <span>{formatDistanceToNow(activity.timestamp, { addSuffix: true })}</span>
            </div>
            
            {activity.metadata?.subject && (
              <div className="flex items-center gap-1">
                <BookOpen className="h-3 w-3" />
                <span>{activity.metadata.subject}</span>
              </div>
            )}
            
            {activity.metadata?.teacherName && (
              <div className="flex items-center gap-1">
                <User className="h-3 w-3" />
                <span>{activity.metadata.teacherName}</span>
              </div>
            )}

            {activity.metadata?.grade && (
              <div className="flex items-center gap-1">
                <Award className="h-3 w-3" />
                <span className="font-medium">{activity.metadata.grade}%</span>
              </div>
            )}
          </div>

          {/* Action link */}
          {activity.actionUrl && (
            <div className="mt-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <Button
                variant="ghost"
                size="sm"
                className="text-xs text-blue-600 hover:text-blue-700 h-6 px-2"
              >
                <Eye className="h-3 w-3 mr-1" />
                View Details
                <ExternalLink className="h-3 w-3 ml-1" />
              </Button>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
}

function ActivityFeedSkeleton() {
  return (
    <div className="space-y-4">
      {[...Array(5)].map((_, i) => (
        <div key={i} className="flex items-start gap-3 p-4 rounded-lg border border-slate-200">
          <div className="w-10 h-10 bg-slate-200 rounded-full animate-pulse" />
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-slate-200 rounded animate-pulse w-3/4" />
            <div className="h-3 bg-slate-200 rounded animate-pulse w-full" />
            <div className="h-3 bg-slate-200 rounded animate-pulse w-1/2" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function EnhancedActivityFeed({
  activities,
  title = "Recent Activity",
  maxItems = 10,
  showViewAll = true,
  onViewAll,
  onItemClick,
  className,
  isLoading = false
}: EnhancedActivityFeedProps) {
  const displayedActivities = activities.slice(0, maxItems);
  const hasMoreItems = activities.length > maxItems;

  return (
    <Card className={cn("bg-white/95 backdrop-blur-sm shadow-xl border-slate-200/60", className)}>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="text-xl font-bold bg-gradient-to-r from-slate-900 to-blue-800 bg-clip-text text-transparent">
            {title}
          </CardTitle>
          {showViewAll && onViewAll && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onViewAll}
              className="text-blue-600 hover:text-blue-700 hover:bg-blue-50 transition-colors"
            >
              View All
              <ExternalLink className="h-4 w-4 ml-1" />
            </Button>
          )}
        </div>
        {activities.length > 0 && (
          <p className="text-sm text-slate-600">
            {activities.length} {activities.length === 1 ? 'activity' : 'activities'} 
            {hasMoreItems && ` (showing ${maxItems})`}
          </p>
        )}
      </CardHeader>
      
      <CardContent className="pt-0">
        {isLoading ? (
          <ActivityFeedSkeleton />
        ) : activities.length === 0 ? (
          <div className="text-center py-8">
            <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Clock className="h-8 w-8 text-slate-400" />
            </div>
            <h3 className="font-medium text-slate-900 mb-2">No recent activity</h3>
            <p className="text-sm text-slate-500">
              When you have activity, it will appear here.
            </p>
          </div>
        ) : (
          <ScrollArea className="h-[400px] pr-4">
            <div className="space-y-3">
              <AnimatePresence>
                {displayedActivities.map((activity, index) => (
                  <React.Fragment key={activity.id}>
                    <ActivityItemComponent 
                      activity={activity} 
                      onClick={onItemClick}
                    />
                    {index < displayedActivities.length - 1 && (
                      <Separator className="bg-slate-200/60" />
                    )}
                  </React.Fragment>
                ))}
              </AnimatePresence>
            </div>
          </ScrollArea>
        )}
        
        {hasMoreItems && showViewAll && onViewAll && (
          <div className="mt-4 pt-4 border-t border-slate-200/60">
            <Button
              variant="outline"
              onClick={onViewAll}
              className="w-full hover:bg-blue-50 hover:text-blue-700 hover:border-blue-300 transition-colors"
            >
              View All {activities.length} Activities
              <ExternalLink className="h-4 w-4 ml-2" />
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
