import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Bell, 
  X, 
  Check,
  Trash2,
  Clock
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAppSelector, useAppDispatch } from '@/stores/store';
import { markAsRead as markAsReadLocal, removeNotification as removeNotificationLocal, markAllAsRead as markAllAsReadLocal, mergeBackendNotifications } from '@/stores/notificationSlice';
import { useNotifications, useMarkAsRead, useDeleteNotification } from '../hooks/use-notifications';
import { useAuth } from '@/hooks/useAuth';
import { getRoleClasses } from '@/lib/theme';
import type { Notification } from '@/types/notification';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';
import { useEffect } from 'react';

interface NotificationCenterProps {
  userId: string;
  isOpen: boolean;
  onClose: () => void;
  className?: string;
}

export function NotificationCenter({ userId, isOpen, onClose, className }: NotificationCenterProps) {
  const { user } = useAuth();
  const roleClasses = getRoleClasses(user?.role);
  const dispatch = useAppDispatch();
  
  // Use backend API for data fetching and Redux store for real-time updates
  const { data: backendNotifications, isLoading, refetch } = useNotifications(userId, {
    page: 0,
    size: 20
  });
  
  const { list: realtimeNotifications, unreadCount } = useAppSelector(state => state.notification);
  const markAsReadMutation = useMarkAsRead();
  const deleteNotificationMutation = useDeleteNotification();

  // Ensure backendNotifications is an array
  const safeBackendNotifications = Array.isArray(backendNotifications) ? backendNotifications : [];

  // Use merged notifications from Redux store
  const notifications = realtimeNotifications;

  // Update Redux store when backend data loads (but don't override WebSocket notifications)
  useEffect(() => {
    if (safeBackendNotifications.length > 0) {
      dispatch(mergeBackendNotifications(safeBackendNotifications));
    }
  }, [safeBackendNotifications, dispatch]);

  const isBackendNotification = (id: string): boolean => {
    // Backend notifications have numeric IDs, WebSocket notifications have string IDs
    return /^\d+$/.test(id);
  };

  const handleMarkAsRead = async (notificationId: string) => {
    try {
      // Only call backend for notifications that came from backend
      if (isBackendNotification(notificationId)) {
        await markAsReadMutation.mutateAsync({ notificationId });
      }
      
      // Always update local state
      dispatch(markAsReadLocal(notificationId));
      toast.success('Marked as read', { duration: 2000 });
    } catch (error) {
      toast.error('Failed to mark as read');
    }
  };

  const handleDeleteNotification = async (notificationId: string) => {
    try {
      // Only call backend for notifications that came from backend
      if (isBackendNotification(notificationId)) {
        await deleteNotificationMutation.mutateAsync({ notificationId });
      }
      
      // Always update local state
      dispatch(removeNotificationLocal(notificationId));
      toast.success('Notification deleted', { duration: 2000 });
    } catch (error) {
      toast.error('Failed to delete notification');
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      // Mark backend notifications as read
      const unreadBackendIds = notifications
        .filter(n => !(n.read || n.readAt) && isBackendNotification(n.id))
        .map(n => n.id);
      
      if (unreadBackendIds.length > 0) {
        await Promise.all(unreadBackendIds.map(id => 
          markAsReadMutation.mutateAsync({ notificationId: id })
        ));
      }
      
      // Update local state (handles both backend and WebSocket notifications)
      dispatch(markAllAsReadLocal());
      toast.success('All notifications marked as read', { duration: 2000 });
    } catch (error) {
      toast.error('Failed to mark all as read');
    }
  };

  if (!isOpen) return null;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95, y: -10 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95, y: -10 }}
      transition={{ duration: 0.2, ease: "easeOut" }}
      className={`absolute top-full right-0 mt-3 w-96 bg-white rounded-xl shadow-2xl border border-gray-200 z-50 overflow-hidden max-h-[500px] flex flex-col ${className}`}
    >
      {/* Header */}
      <div className={`px-6 py-4 border-b border-gray-100 ${roleClasses.gradient}`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`p-2 ${roleClasses.primaryLight} rounded-lg`}>
              <Bell className={`h-5 w-5 ${roleClasses.icon}`} />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">Notifications</h3>
              {unreadCount > 0 && (
                <p className="text-sm text-gray-600">
                  {unreadCount} unread
                </p>
              )}
            </div>
          </div>
          <Button 
            variant="ghost" 
            size="sm" 
            onClick={onClose}
            className="h-8 w-8 p-0 hover:bg-white/60 rounded-lg transition-colors text-gray-500 hover:text-gray-700"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="max-h-80 overflow-y-auto scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100 relative">
        {isLoading ? (
          <div className="p-8 text-center">
            <div className={`animate-spin rounded-full h-8 w-8 border-b-2 ${roleClasses.primaryBg.replace('bg-', 'border-')} mx-auto mb-4`}></div>
            <p className="text-gray-500 text-sm">Loading notifications...</p>
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-12 text-center">
            <div className="p-4 bg-gray-50 rounded-full w-16 h-16 mx-auto mb-4 flex items-center justify-center">
              <Bell className="h-8 w-8 text-gray-300" />
            </div>
            <h4 className="font-medium text-gray-900 mb-1">No notifications</h4>
            <p className="text-sm text-gray-500">You're all caught up!</p>
          </div>
        ) : (
          <div className="p-2 space-y-2">
            <AnimatePresence>
              {notifications.map((notification, index) => (
                  <motion.div
                    key={notification.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, x: -100 }}
                    transition={{ delay: index * 0.05, duration: 0.3 }}
                    className={`group relative p-3 rounded-lg border transition-all duration-200 hover:shadow-md ${
                      (notification.read || notification.readAt)
                        ? 'bg-white border-gray-100 hover:border-gray-200' 
                        : `${roleClasses.primaryLight} ${roleClasses.primaryBorder} hover:shadow-sm`
                    }`}
                  >
                    {/* Unread indicator */}
                    {!(notification.read || notification.readAt) && (
                      <div className={`absolute top-3 left-2 w-2 h-2 ${roleClasses.primaryBg} rounded-full`}></div>
                    )}
                    
                    <div className="flex justify-between items-start gap-3 ml-3">
                      <div className="flex-1 min-w-0">
                        <h4 className={`font-medium mb-1 leading-tight ${
                          (notification.read || notification.readAt) ? 'text-gray-700' : 'text-gray-900'
                        }`}>
                          {notification.title}
                        </h4>
                        <p className={`text-sm mb-2 leading-relaxed line-clamp-2 ${
                          (notification.read || notification.readAt) ? 'text-gray-500' : 'text-gray-600'
                        }`}>
                          {notification.message || notification.body || 'No message'}
                        </p>
                        <div className="flex items-center gap-2 text-xs text-gray-400">
                          <Clock className="h-3 w-3" />
                          <span>
                            {notification.timestamp 
                              ? formatDistanceToNow(new Date(notification.timestamp), { addSuffix: true })
                              : notification.createdAt
                              ? formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })
                              : 'Just now'
                            }
                          </span>
                        </div>
                      </div>
                      
                      {/* Action buttons */}
                      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                        {!(notification.read || notification.readAt) && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkAsRead(notification.id)}
                            className="h-8 w-8 p-0 hover:bg-green-100 hover:text-green-600 rounded-lg transition-colors text-gray-400"
                            title="Mark as read"
                          >
                            <Check className="h-4 w-4" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteNotification(notification.id)}
                          className="h-8 w-8 p-0 hover:bg-red-100 hover:text-red-600 rounded-lg transition-colors text-gray-400"
                          title="Delete notification"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>
          )}
        {/* Scroll indicator gradient */}
        <div className="absolute bottom-0 left-0 right-0 h-4 bg-gradient-to-t from-white to-transparent pointer-events-none"></div>
      </div>

      {/* Footer */}
      {notifications.length > 0 && (
        <div className="px-6 py-3 border-t border-gray-100 bg-gray-50/50">
          <div className="flex justify-between items-center">
            <p className="text-xs text-gray-500">
              Showing {notifications.length} notifications
            </p>
            {unreadCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleMarkAllAsRead}
                className="text-xs h-auto p-1 hover:bg-gray-100 text-gray-600 hover:text-gray-900"
              >
                Mark all read
              </Button>
            )}
          </div>
        </div>
      )}
    </motion.div>
  );
}