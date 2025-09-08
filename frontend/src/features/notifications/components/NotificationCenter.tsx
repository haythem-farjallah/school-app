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
import { ScrollArea } from '@/components/ui/scroll-area';
import { useNotifications, useUnreadNotifications, useMarkAsRead, useDeleteNotification } from '../hooks/use-notifications';
import { useAuth } from '@/hooks/useAuth';
import { getRoleClasses } from '@/lib/theme';
import type { Notification } from '@/types/notification';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';

interface NotificationCenterProps {
  userId: string;
  isOpen: boolean;
  onClose: () => void;
  className?: string;
}

export function NotificationCenter({ userId, isOpen, onClose, className }: NotificationCenterProps) {
  const { user } = useAuth();
  const roleClasses = getRoleClasses(user?.role);
  
  // Hooks
  const { data: notifications = [], isLoading } = useNotifications(userId, {
    page: 0,
    size: 20
  });
  
  const { data: unreadNotifications = [] } = useUnreadNotifications(userId);
  const safeNotifications = Array.isArray(notifications) ? notifications : [];
  const safeUnreadNotifications = Array.isArray(unreadNotifications) ? unreadNotifications : [];
  
  const markAsReadMutation = useMarkAsRead();
  const deleteNotificationMutation = useDeleteNotification();

  const handleMarkAsRead = async (notificationId: string) => {
    try {
      await markAsReadMutation.mutateAsync({ notificationId });
      toast.success('Marked as read', { duration: 2000 });
    } catch (error) {
      toast.error('Failed to mark as read');
    }
  };

  const handleDeleteNotification = async (notificationId: string) => {
    try {
      await deleteNotificationMutation.mutateAsync({ notificationId });
      toast.success('Notification deleted', { duration: 2000 });
    } catch (error) {
      toast.error('Failed to delete notification');
    }
  };

  if (!isOpen) return null;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95, y: -10 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95, y: -10 }}
      transition={{ duration: 0.2, ease: "easeOut" }}
      className={`absolute top-full right-0 mt-3 w-96 bg-white rounded-xl shadow-2xl border border-gray-200 z-50 overflow-hidden ${className}`}
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
              {safeUnreadNotifications.length > 0 && (
                <p className="text-sm text-gray-600">
                  {safeUnreadNotifications.length} unread
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
      <div className="max-h-96">
        <ScrollArea className="h-full">
          {isLoading ? (
            <div className="p-8 text-center">
              <div className={`animate-spin rounded-full h-8 w-8 border-b-2 ${roleClasses.primaryBg.replace('bg-', 'border-')} mx-auto mb-4`}></div>
              <p className="text-gray-500 text-sm">Loading notifications...</p>
            </div>
          ) : safeNotifications.length === 0 ? (
            <div className="p-12 text-center">
              <div className="p-4 bg-gray-50 rounded-full w-16 h-16 mx-auto mb-4 flex items-center justify-center">
                <Bell className="h-8 w-8 text-gray-300" />
              </div>
              <h4 className="font-medium text-gray-900 mb-1">No notifications</h4>
              <p className="text-sm text-gray-500">You're all caught up!</p>
            </div>
          ) : (
            <div className="p-2">
              <AnimatePresence>
                {safeNotifications.map((notification, index) => (
                  <motion.div
                    key={notification.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, x: -100 }}
                    transition={{ delay: index * 0.05, duration: 0.3 }}
                    className={`group relative p-4 m-2 rounded-xl border transition-all duration-200 hover:shadow-md ${
                      notification.readAt 
                        ? 'bg-white border-gray-100 hover:border-gray-200' 
                        : `${roleClasses.primaryLight} ${roleClasses.primaryBorder} hover:shadow-sm`
                    }`}
                  >
                    {/* Unread indicator */}
                    {!notification.readAt && (
                      <div className={`absolute top-4 left-2 w-2 h-2 ${roleClasses.primaryBg} rounded-full`}></div>
                    )}
                    
                    <div className="flex justify-between items-start gap-3 ml-2">
                      <div className="flex-1 min-w-0">
                        <h4 className={`font-medium mb-2 leading-snug ${
                          notification.readAt ? 'text-gray-700' : 'text-gray-900'
                        }`}>
                          {notification.title}
                        </h4>
                        <p className={`text-sm mb-3 leading-relaxed ${
                          notification.readAt ? 'text-gray-500' : 'text-gray-600'
                        }`}>
                          {notification.body}
                        </p>
                        <div className="flex items-center gap-2 text-xs text-gray-400">
                          <Clock className="h-3 w-3" />
                          <span>
                            {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
                          </span>
                        </div>
                      </div>
                      
                      {/* Action buttons */}
                      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                        {!notification.readAt && (
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
        </ScrollArea>
      </div>

      {/* Footer */}
      {safeNotifications.length > 0 && (
        <div className="px-6 py-3 border-t border-gray-100 bg-gray-50/50">
          <p className="text-xs text-gray-500 text-center">
            Showing {safeNotifications.length} notifications
          </p>
        </div>
      )}
    </motion.div>
  );
}