import { useEffect, useRef, useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/hooks/useAuth";
import toast from "react-hot-toast";
import type { UserRole } from "@/hooks/useAuth";

// Real-time event types
interface RealtimeEvent {
  type: string;
  data: Record<string, unknown>;
  timestamp: string;
  userId?: number;
  role?: UserRole;
}

// Specific event types
interface GradeUpdateEvent extends RealtimeEvent {
  type: "GRADE_UPDATED" | "GRADE_CREATED";
  data: {
    studentId: number;
    courseId: number;
    grade: number;
    courseName: string;
    studentName: string;
  };
}

interface AnnouncementEvent extends RealtimeEvent {
  type: "ANNOUNCEMENT_CREATED" | "ANNOUNCEMENT_UPDATED";
  data: {
    id: number;
    title: string;
    importance: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
    targetRoles: UserRole[];
  };
}

interface AttendanceEvent extends RealtimeEvent {
  type: "ATTENDANCE_MARKED" | "ATTENDANCE_UPDATED";
  data: {
    classId: number;
    className: string;
    date: string;
    present: number;
    absent: number;
  };
}

interface ScheduleEvent extends RealtimeEvent {
  type: "SCHEDULE_UPDATED" | "CLASS_CANCELLED";
  data: {
    classId: number;
    className: string;
    courseId: number;
    courseName: string;
    date: string;
    time: string;
    reason?: string;
  };
}

interface EnrollmentEvent extends RealtimeEvent {
  type: "ENROLLMENT_CREATED" | "ENROLLMENT_UPDATED" | "ENROLLMENT_CANCELLED";
  data: {
    studentId: number;
    studentName: string;
    classId: number;
    className: string;
    status: string;
  };
}

type DashboardRealtimeEvent = 
  | GradeUpdateEvent 
  | AnnouncementEvent 
  | AttendanceEvent 
  | ScheduleEvent 
  | EnrollmentEvent;

interface UseRealtimeUpdatesOptions {
  enableNotifications?: boolean;
  enableToasts?: boolean;
  enableQueryInvalidation?: boolean;
  eventTypes?: string[];
}

/**
 * Hook for managing real-time updates via WebSocket
 */
export function useRealtimeUpdates(options: UseRealtimeUpdatesOptions = {}) {
  const {
    enableNotifications = true,
    enableToasts = true,
    enableQueryInvalidation = true,
    eventTypes = [],
  } = options;

  const { user } = useAuth();
  const queryClient = useQueryClient();
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  // Handle incoming WebSocket messages
  const handleMessage = useCallback((event: MessageEvent) => {
    try {
      const realtimeEvent: DashboardRealtimeEvent = JSON.parse(event.data);
      
      // Filter events by type if specified
      if (eventTypes.length > 0 && !eventTypes.includes(realtimeEvent.type)) {
        return;
      }

      // Filter events by user/role if applicable
      if (realtimeEvent.userId && realtimeEvent.userId !== user?.id) {
        return;
      }

      if (realtimeEvent.role && realtimeEvent.role !== user?.role) {
        return;
      }

      console.log("📡 Realtime event received:", realtimeEvent);

      // Handle event-specific logic
      handleRealtimeEvent(realtimeEvent);

    } catch (error) {
      console.error("Failed to parse WebSocket message:", error);
    }
  }, [user, eventTypes, enableNotifications, enableToasts, enableQueryInvalidation, queryClient]);

  // Handle specific realtime events
  const handleRealtimeEvent = useCallback((event: DashboardRealtimeEvent) => {
    switch (event.type) {
      case "GRADE_UPDATED":
      case "GRADE_CREATED":
        handleGradeEvent(event as GradeUpdateEvent);
        break;
      
      case "ANNOUNCEMENT_CREATED":
      case "ANNOUNCEMENT_UPDATED":
        handleAnnouncementEvent(event as AnnouncementEvent);
        break;
      
      case "ATTENDANCE_MARKED":
      case "ATTENDANCE_UPDATED":
        handleAttendanceEvent(event as AttendanceEvent);
        break;
      
      case "SCHEDULE_UPDATED":
      case "CLASS_CANCELLED":
        handleScheduleEvent(event as ScheduleEvent);
        break;
      
      case "ENROLLMENT_CREATED":
      case "ENROLLMENT_UPDATED":
      case "ENROLLMENT_CANCELLED":
        handleEnrollmentEvent(event as EnrollmentEvent);
        break;
      
      default:
        console.log("🔄 Unhandled realtime event:", event);
    }

    // Generic query invalidation
    if (enableQueryInvalidation) {
      invalidateRelevantQueries(event);
    }
  }, [enableNotifications, enableToasts, enableQueryInvalidation, queryClient, user]);

  // Event-specific handlers
  const handleGradeEvent = useCallback((event: GradeUpdateEvent) => {
    if (enableToasts) {
      const action = event.type === "GRADE_CREATED" ? "added" : "updated";
      toast.success(
        `Grade ${action} for ${event.data.studentName} in ${event.data.courseName}`,
        { duration: 4000 }
      );
    }

    if (enableNotifications && "Notification" in window && Notification.permission === "granted") {
      new Notification("Grade Update", {
        body: `New grade ${event.type === "GRADE_CREATED" ? "added" : "updated"} for ${event.data.courseName}`,
        icon: "/favicon.ico",
      });
    }
  }, [enableToasts, enableNotifications]);

  const handleAnnouncementEvent = useCallback((event: AnnouncementEvent) => {
    // Only show if user's role is in target roles
    if (user?.role && event.data.targetRoles.includes(user.role)) {
      if (enableToasts) {
        toast.success(`New announcement: ${event.data.title}`, { duration: 5000 });
      }

      if (enableNotifications && "Notification" in window && Notification.permission === "granted") {
        new Notification("New Announcement", {
          body: event.data.title,
          icon: "/favicon.ico",
        });
      }
    }
  }, [enableToasts, enableNotifications, user]);

  const handleAttendanceEvent = useCallback((event: AttendanceEvent) => {
    if (user?.role === "TEACHER" && enableToasts) {
      toast.success(
        `Attendance ${event.type === "ATTENDANCE_MARKED" ? "marked" : "updated"} for ${event.data.className}`,
        { duration: 3000 }
      );
    }
  }, [enableToasts, user]);

  const handleScheduleEvent = useCallback((event: ScheduleEvent) => {
    if (enableToasts) {
      const message = event.type === "CLASS_CANCELLED" 
        ? `Class cancelled: ${event.data.courseName} (${event.data.className})`
        : `Schedule updated: ${event.data.courseName}`;
      
      toast(message, { duration: 4000 });
    }
  }, [enableToasts]);

  const handleEnrollmentEvent = useCallback((event: EnrollmentEvent) => {
    if (user?.role === "STAFF" && enableToasts) {
      const action = event.type.split("_")[1].toLowerCase(); // created, updated, cancelled
      toast.success(
        `Enrollment ${action}: ${event.data.studentName} in ${event.data.className}`,
        { duration: 3000 }
      );
    }
  }, [enableToasts, user]);

  // Invalidate relevant queries based on event type
  const invalidateRelevantQueries = useCallback((event: DashboardRealtimeEvent) => {
    const queriesToInvalidate: (string | number)[][] = [];

    switch (event.type) {
      case "GRADE_UPDATED":
      case "GRADE_CREATED":
        if (user?.role && user?.id) {
          queriesToInvalidate.push(
            ["dashboard", user.role, user.id],
            ["grades"],
            ["stats", user.role, user.id]
          );
        }
        break;
      
      case "ANNOUNCEMENT_CREATED":
      case "ANNOUNCEMENT_UPDATED":
        queriesToInvalidate.push(["announcements"]);
        if (user?.id) {
          queriesToInvalidate.push(["notifications", user.id]);
        }
        break;
      
      case "ATTENDANCE_MARKED":
      case "ATTENDANCE_UPDATED":
        if (user?.role && user?.id) {
          queriesToInvalidate.push(
            ["dashboard", user.role, user.id],
            ["attendance"],
            ["stats", user.role, user.id]
          );
        }
        break;
      
      case "SCHEDULE_UPDATED":
      case "CLASS_CANCELLED":
        if (user?.role && user?.id) {
          queriesToInvalidate.push(
            ["schedule", user.role, user.id],
            ["dashboard", user.role, user.id]
          );
        }
        break;
      
      case "ENROLLMENT_CREATED":
      case "ENROLLMENT_UPDATED":
      case "ENROLLMENT_CANCELLED":
        if (user?.role && user?.id) {
          queriesToInvalidate.push(
            ["dashboard", user.role, user.id],
            ["enrollments"],
            ["stats", user.role, user.id]
          );
        }
        break;
    }

    // Invalidate all relevant queries
    queriesToInvalidate.forEach(queryKey => {
      queryClient.invalidateQueries({ queryKey });
    });
  }, [queryClient, user]);

  // WebSocket connection management
  const connect = useCallback(() => {
    if (!user?.id) return;

    try {
      const wsUrl = `${process.env.REACT_APP_WS_URL || "ws://localhost:8080"}/ws/dashboard/${user.id}`;
      wsRef.current = new WebSocket(wsUrl);

      wsRef.current.onopen = () => {
        console.log("🔗 WebSocket connected for dashboard updates");
        reconnectAttempts.current = 0;
        
        if (enableToasts) {
          toast.success("Connected to real-time updates", { duration: 2000 });
        }
      };

      wsRef.current.onmessage = handleMessage;

      wsRef.current.onclose = (event) => {
        console.log("🔌 WebSocket disconnected:", event.code, event.reason);
        
        // Attempt to reconnect if not manually closed
        if (event.code !== 1000 && reconnectAttempts.current < maxReconnectAttempts) {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000);
          reconnectTimeoutRef.current = setTimeout(() => {
            reconnectAttempts.current += 1;
            console.log(`🔄 Attempting to reconnect... (${reconnectAttempts.current}/${maxReconnectAttempts})`);
            connect();
          }, delay);
        } else if (reconnectAttempts.current >= maxReconnectAttempts) {
          if (enableToasts) {
            toast.error("Lost connection to real-time updates", { duration: 5000 });
          }
        }
      };

      wsRef.current.onerror = (error) => {
        console.error("❌ WebSocket error:", error);
      };

    } catch (error) {
      console.error("Failed to connect to WebSocket:", error);
    }
  }, [user, handleMessage, enableToasts]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (wsRef.current) {
      wsRef.current.close(1000, "Manual disconnect");
      wsRef.current = null;
    }
  }, []);

  // Request notification permission
  const requestNotificationPermission = useCallback(async () => {
    if ("Notification" in window && Notification.permission === "default") {
      const permission = await Notification.requestPermission();
      return permission === "granted";
    }
    return Notification.permission === "granted";
  }, []);

  // Setup and cleanup
  useEffect(() => {
    if (user?.id) {
      connect();
      
      if (enableNotifications) {
        requestNotificationPermission();
      }
    }

    return () => {
      disconnect();
    };
  }, [user?.id, connect, disconnect, enableNotifications, requestNotificationPermission]);

  return {
    isConnected: wsRef.current?.readyState === WebSocket.OPEN,
    connect,
    disconnect,
    requestNotificationPermission,
  };
}

export type { DashboardRealtimeEvent, RealtimeEvent };
