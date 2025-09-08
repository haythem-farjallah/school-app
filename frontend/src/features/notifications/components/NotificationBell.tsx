import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bell, BellRing } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { NotificationCenter } from './NotificationCenter';
import { useUnreadNotifications } from '../hooks/use-notifications';
import { useAuth } from '@/hooks/useAuth';
import { getRoleClasses } from '@/lib/theme';

interface NotificationBellProps {
  className?: string;
}

export function NotificationBell({ className }: NotificationBellProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [hasNewNotification, setHasNewNotification] = useState(false);
  const { user } = useAuth();
  const userId = user?.id;
  const roleClasses = getRoleClasses(user?.role);
  
  const { data: unreadNotifications = [] } = useUnreadNotifications(userId);
  const safeUnreadNotifications = Array.isArray(unreadNotifications) ? unreadNotifications : [];
  const unreadCount = safeUnreadNotifications.length;

  // Animate when new notifications arrive
  useEffect(() => {
    if (unreadCount > 0) {
      setHasNewNotification(true);
      const timer = setTimeout(() => setHasNewNotification(false), 1000);
      return () => clearTimeout(timer);
    }
  }, [unreadCount]);

  // Close notification center when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Element;
      if (isOpen && !target.closest('[data-notification-center]')) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  return (
    <div className={`relative ${className}`} data-notification-center>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => setIsOpen(!isOpen)}
        className={`relative p-2.5 hover:bg-gray-100 rounded-xl transition-all duration-200 ${
          isOpen ? 'bg-gray-100 shadow-sm' : ''
        } ${unreadCount > 0 ? `hover:${roleClasses.primaryLight}` : ''}`}
      >
        <motion.div
          animate={hasNewNotification ? { 
            scale: [1, 1.1, 1],
            rotate: [0, -5, 5, 0]
          } : {}}
          transition={{ duration: 0.5, ease: "easeInOut" }}
          className="relative"
        >
          {/* Bell icon with role-based coloring */}
          {unreadCount > 0 ? (
            <motion.div
              animate={{ 
                scale: [1, 1.05, 1],
              }}
              transition={{ 
                duration: 2,
                repeat: Infinity,
                ease: "easeInOut"
              }}
            >
              <BellRing className={`h-5 w-5 ${roleClasses.icon}`} />
            </motion.div>
          ) : (
            <Bell className="h-5 w-5 text-gray-600" />
          )}
          
          {/* Notification badge with role-based coloring */}
          <AnimatePresence>
            {unreadCount > 0 && (
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0, opacity: 0 }}
                transition={{ 
                  type: "spring", 
                  stiffness: 400, 
                  damping: 25 
                }}
                className="absolute -top-1.5 -right-1.5"
              >
                <Badge 
                  className={`h-5 w-5 flex items-center justify-center p-0 text-xs font-semibold shadow-lg border-2 border-white ${roleClasses.primaryBg} text-white`}
                >
                  {unreadCount > 9 ? '9+' : unreadCount}
                </Badge>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Pulse effect with role-based coloring */}
          <AnimatePresence>
            {hasNewNotification && unreadCount > 0 && (
              <motion.div
                initial={{ scale: 1, opacity: 0.6 }}
                animate={{ scale: 2.5, opacity: 0 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 1, ease: "easeOut" }}
                className={`absolute inset-0 ${roleClasses.primaryBg} rounded-full -z-10 opacity-40`}
              />
            )}
          </AnimatePresence>
        </motion.div>
      </Button>

      {/* Notification Center */}
      <AnimatePresence>
        {isOpen && userId && (
          <NotificationCenter
            userId={userId}
            isOpen={isOpen}
            onClose={() => setIsOpen(false)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}