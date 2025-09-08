import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { Calendar, Clock, MapPin, Users } from "lucide-react";

interface ScheduleEvent {
  id: string | number;
  title: string;
  startTime: string; // e.g., "08:00"
  endTime: string;   // e.g., "09:30"
  day: string;       // e.g., "Monday", "Tuesday"
  location?: string;
  participants?: number;
  type?: "class" | "meeting" | "exam" | "break" | "other";
  color?: string;
  description?: string;
}

interface ScheduleGridProps {
  events: ScheduleEvent[];
  title?: string;
  timeSlots?: string[];
  weekdays?: string[];
  isLoading?: boolean;
  onEventClick?: (event: ScheduleEvent) => void;
  className?: string;
  showTimeOnly?: boolean; // For displaying only time-based view
}

const defaultTimeSlots = [
  "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
  "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00"
];

const defaultWeekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];

const eventTypeStyles = {
  class: {
    bg: "bg-blue-100 border-blue-300",
    text: "text-blue-800",
    badge: "bg-blue-600",
  },
  meeting: {
    bg: "bg-purple-100 border-purple-300", 
    text: "text-purple-800",
    badge: "bg-purple-600",
  },
  exam: {
    bg: "bg-red-100 border-red-300",
    text: "text-red-800", 
    badge: "bg-red-600",
  },
  break: {
    bg: "bg-green-100 border-green-300",
    text: "text-green-800",
    badge: "bg-green-600",
  },
  other: {
    bg: "bg-gray-100 border-gray-300",
    text: "text-gray-800",
    badge: "bg-gray-600",
  },
};

export function ScheduleGrid({
  events,
  title = "Schedule",
  timeSlots = defaultTimeSlots,
  weekdays = defaultWeekdays,
  isLoading = false,
  onEventClick,
  className = "",
  showTimeOnly = false,
}: ScheduleGridProps) {

  // Helper function to get events for a specific time slot and day
  const getEventsForSlot = (timeSlot: string, day: string) => {
    return events.filter(event => {
      const eventStart = event.startTime;
      const eventEnd = event.endTime;
      const eventDay = event.day;
      
      return eventDay === day && eventStart <= timeSlot && eventEnd > timeSlot;
    });
  };

  // Helper function to calculate event height based on duration
  const getEventHeight = (event: ScheduleEvent) => {
    const startIndex = timeSlots.indexOf(event.startTime);
    const endIndex = timeSlots.findIndex(slot => slot >= event.endTime);
    const duration = endIndex - startIndex;
    return Math.max(duration * 0.5, 1); // Minimum height of 1 unit
  };

  if (isLoading) {
    return <ScheduleGridSkeleton title={title} />;
  }

  if (showTimeOnly) {
    return <TimeOnlySchedule events={events} timeSlots={timeSlots} onEventClick={onEventClick} />;
  }

  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            {title}
          </CardTitle>
          <Badge variant="outline" className="text-xs">
            {events.length} events
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <div className="min-w-[800px]">
            {/* Header Row - Weekdays */}
            <div className="grid grid-cols-6 border-b bg-gray-50">
              <div className="p-3 border-r font-medium text-sm text-gray-600">
                Time
              </div>
              {weekdays.map((day) => (
                <div key={day} className="p-3 border-r font-medium text-sm text-center text-gray-800">
                  {day}
                </div>
              ))}
            </div>

            {/* Time Slots */}
            {timeSlots.map((timeSlot, timeIndex) => (
              <div key={timeSlot} className="grid grid-cols-6 border-b min-h-[60px]">
                {/* Time Column */}
                <div className="p-3 border-r bg-gray-50 flex items-center">
                  <div className="text-sm font-medium text-gray-600">
                    {timeSlot}
                  </div>
                </div>

                {/* Day Columns */}
                {weekdays.map((day) => {
                  const slotEvents = getEventsForSlot(timeSlot, day);
                  
                  return (
                    <div key={`${day}-${timeSlot}`} className="p-1 border-r relative min-h-[60px]">
                      {slotEvents.map((event) => {
                        const isEventStart = event.startTime === timeSlot;
                        
                        if (!isEventStart) return null; // Only render at start time
                        
                        const eventStyle = eventTypeStyles[event.type || 'other'];
                        
                        return (
                          <div
                            key={event.id}
                            className={cn(
                              "absolute inset-x-1 p-2 rounded-md border cursor-pointer transition-all duration-200 hover:shadow-md z-10",
                              eventStyle.bg,
                              eventStyle.text,
                              onEventClick && "hover:scale-105"
                            )}
                            style={{
                              height: `${getEventHeight(event) * 60}px`,
                            }}
                            onClick={() => onEventClick?.(event)}
                          >
                            <div className="space-y-1">
                              <div className="font-medium text-xs leading-tight">
                                {event.title}
                              </div>
                              
                              <div className="flex items-center gap-1 text-xs opacity-80">
                                <Clock className="h-3 w-3" />
                                {event.startTime} - {event.endTime}
                              </div>
                              
                              {event.location && (
                                <div className="flex items-center gap-1 text-xs opacity-80">
                                  <MapPin className="h-3 w-3" />
                                  {event.location}
                                </div>
                              )}
                              
                              {event.participants && (
                                <div className="flex items-center gap-1 text-xs opacity-80">
                                  <Users className="h-3 w-3" />
                                  {event.participants}
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

// Time-only view for simpler layouts
function TimeOnlySchedule({ 
  events, 
  timeSlots, 
  onEventClick 
}: { 
  events: ScheduleEvent[]; 
  timeSlots: string[];
  onEventClick?: (event: ScheduleEvent) => void;
}) {
  const todayEvents = events.filter(event => {
    const today = new Date().toLocaleDateString('en-US', { weekday: 'long' });
    return event.day === today;
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Clock className="h-5 w-5" />
          Today's Schedule
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {todayEvents.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No events scheduled for today
          </div>
        ) : (
          todayEvents.map((event) => {
            const eventStyle = eventTypeStyles[event.type || 'other'];
            
            return (
              <div
                key={event.id}
                className={cn(
                  "p-3 rounded-lg border cursor-pointer transition-all duration-200 hover:shadow-md",
                  eventStyle.bg,
                  eventStyle.text,
                  onEventClick && "hover:scale-[1.02]"
                )}
                onClick={() => onEventClick?.(event)}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium">{event.title}</div>
                    <div className="text-sm opacity-80">
                      {event.startTime} - {event.endTime}
                    </div>
                  </div>
                  
                  <Badge className={cn("text-white", eventStyle.badge)}>
                    {event.type || 'event'}
                  </Badge>
                </div>
                
                {event.location && (
                  <div className="flex items-center gap-1 text-sm opacity-80 mt-2">
                    <MapPin className="h-3 w-3" />
                    {event.location}
                  </div>
                )}
              </div>
            );
          })
        )}
      </CardContent>
    </Card>
  );
}

// Loading skeleton
function ScheduleGridSkeleton({ title }: { title: string }) {
  return (
    <Card>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            <Skeleton className="h-6 w-24" />
          </div>
          <Skeleton className="h-5 w-16" />
        </div>
      </CardHeader>
      
      <CardContent className="space-y-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="grid grid-cols-6 gap-3">
            <Skeleton className="h-12 w-full" />
            {Array.from({ length: 5 }).map((_, j) => (
              <Skeleton key={j} className="h-12 w-full" />
            ))}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

export type { ScheduleEvent, ScheduleGridProps };
