import React from 'react';
import { Helmet } from 'react-helmet-async';
import { EnhancedAnnouncementSystem } from '@/features/notifications/components/EnhancedAnnouncementSystem';

export default function EnhancedAnnouncementsPage() {
  return (
    <>
      <Helmet>
        <title>Announcements - School Management System</title>
        <meta name="description" content="Create and manage school announcements with targeted delivery across multiple channels" />
      </Helmet>
      
      <div className="container mx-auto p-6">
        <EnhancedAnnouncementSystem />
      </div>
    </>
  );
}
