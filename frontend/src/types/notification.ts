/** A persisted notification as returned by /api/v1/notifications (NotificationDto). */
export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  entityType: string | null;
  entityId: number | null;
  actionUrl: string | null;
  readStatus: boolean;
  createdAt: string;
  readAt: string | null;
}
