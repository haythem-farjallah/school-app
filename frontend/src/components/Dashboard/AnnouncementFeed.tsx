import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { Megaphone, Clock, User, Eye, MoreHorizontal } from "lucide-react";
import { formatDistanceToNow } from "date-fns";

interface Announcement {
  id: string | number;
  title: string;
  body: string;
  importance: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  createdBy?: string;
  createdAt: string; // ISO date string
  isPublic?: boolean;
  isRead?: boolean;
  targetAudience?: string[];
}

interface AnnouncementFeedProps {
  announcements: Announcement[];
  title?: string;
  maxItems?: number;
  isLoading?: boolean;
  onAnnouncementClick?: (announcement: Announcement) => void;
  onViewAll?: () => void;
  className?: string;
  showTargetAudience?: boolean;
}

const importanceStyles = {
  LOW: {
    badge: "bg-green-100 text-green-800 border-green-300",
    card: "border-l-green-400",
    icon: "text-green-600",
  },
  MEDIUM: {
    badge: "bg-blue-100 text-blue-800 border-blue-300",
    card: "border-l-blue-400",
    icon: "text-blue-600",
  },
  HIGH: {
    badge: "bg-orange-100 text-orange-800 border-orange-300", 
    card: "border-l-orange-400",
    icon: "text-orange-600",
  },
  URGENT: {
    badge: "bg-red-100 text-red-800 border-red-300",
    card: "border-l-red-400 animate-pulse",
    icon: "text-red-600",
  },
};

export function AnnouncementFeed({
  announcements,
  title = "Announcements",
  maxItems = 5,
  isLoading = false,
  onAnnouncementClick,
  onViewAll,
  className = "",
  showTargetAudience = false,
}: AnnouncementFeedProps) {

  const displayedAnnouncements = announcements.slice(0, maxItems);
  const hasMore = announcements.length > maxItems;

  if (isLoading) {
    return <AnnouncementFeedSkeleton title={title} maxItems={maxItems} />;
  }

  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Megaphone className="h-5 w-5" />
            {title}
          </CardTitle>
          
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs">
              {announcements.length}
            </Badge>
            
            {onViewAll && hasMore && (
              <Button variant="ghost" size="sm" onClick={onViewAll}>
                View All
              </Button>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-3">
        {displayedAnnouncements.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <Megaphone className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>No announcements at this time</p>
          </div>
        ) : (
          displayedAnnouncements.map((announcement) => (
            <AnnouncementCard
              key={announcement.id}
              announcement={announcement}
              onClick={onAnnouncementClick}
              showTargetAudience={showTargetAudience}
            />
          ))
        )}
        
        {hasMore && onViewAll && (
          <div className="pt-3 border-t">
            <Button 
              variant="outline" 
              className="w-full text-sm"
              onClick={onViewAll}
            >
              View {announcements.length - maxItems} more announcements
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function AnnouncementCard({
  announcement,
  onClick,
  showTargetAudience,
}: {
  announcement: Announcement;
  onClick?: (announcement: Announcement) => void;
  showTargetAudience: boolean;
}) {
  const styles = importanceStyles[announcement.importance];
  
  return (
    <div
      className={cn(
        "p-4 rounded-lg border-l-4 bg-white hover:bg-gray-50/50 transition-colors duration-200",
        styles.card,
        onClick && "cursor-pointer hover:shadow-md",
        !announcement.isRead && "shadow-sm"
      )}
      onClick={() => onClick?.(announcement)}
    >
      <div className="space-y-3">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex-1 space-y-1">
            <div className="flex items-center gap-2">
              <h3 className={cn(
                "font-medium text-sm leading-tight",
                !announcement.isRead && "font-semibold"
              )}>
                {announcement.title}
              </h3>
              
              <Badge variant="outline" className={cn("text-xs", styles.badge)}>
                {announcement.importance}
              </Badge>
              
              {!announcement.isRead && (
                <div className="h-2 w-2 bg-blue-500 rounded-full" />
              )}
            </div>
            
            {showTargetAudience && announcement.targetAudience && (
              <div className="flex items-center gap-1 text-xs text-gray-600">
                <Eye className="h-3 w-3" />
                {announcement.targetAudience.join(", ")}
              </div>
            )}
          </div>
          
          <div className="flex items-center gap-2 text-xs text-gray-500">
            <Clock className="h-3 w-3" />
            {formatDistanceToNow(new Date(announcement.createdAt), { addSuffix: true })}
          </div>
        </div>

        {/* Body */}
        <p className="text-sm text-gray-700 leading-relaxed line-clamp-3">
          {announcement.body}
        </p>

        {/* Footer */}
        <div className="flex items-center justify-between text-xs text-gray-500">
          <div className="flex items-center gap-1">
            <User className="h-3 w-3" />
            {announcement.createdBy || "System"}
          </div>
          
          <div className="flex items-center gap-2">
            {announcement.isPublic && (
              <Badge variant="secondary" className="text-xs">
                Public
              </Badge>
            )}
            
            {onClick && (
              <Button variant="ghost" size="sm" className="h-6 px-2 text-xs">
                <MoreHorizontal className="h-3 w-3" />
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// Loading skeleton
function AnnouncementFeedSkeleton({ 
  title, 
  maxItems 
}: { 
  title: string; 
  maxItems: number;
}) {
  return (
    <Card>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Megaphone className="h-5 w-5" />
            {title}
          </div>
          <Skeleton className="h-5 w-12" />
        </div>
      </CardHeader>

      <CardContent className="space-y-3">
        {Array.from({ length: maxItems }).map((_, i) => (
          <div key={i} className="p-4 border-l-4 border-gray-200 bg-gray-50 rounded-lg">
            <div className="space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-4 w-32" />
                    <Skeleton className="h-4 w-16" />
                  </div>
                  <Skeleton className="h-3 w-24" />
                </div>
                <Skeleton className="h-3 w-16" />
              </div>
              
              <div className="space-y-2">
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-3 w-3/4" />
              </div>
              
              <div className="flex items-center justify-between">
                <Skeleton className="h-3 w-20" />
                <Skeleton className="h-3 w-12" />
              </div>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

export type { Announcement, AnnouncementFeedProps };
