import React from 'react';
import { Helmet } from 'react-helmet-async';
import { CommunicationDashboard } from '@/features/notifications/components/CommunicationDashboard';

export default function CommunicationDashboardPage() {
  return (
    <>
      <Helmet>
        <title>Communication Dashboard - School Management System</title>
        <meta name="description" content="Monitor and analyze your notification system performance across all communication channels" />
      </Helmet>
      
      <div className="container mx-auto p-6">
        <CommunicationDashboard />
      </div>
    </>
  );
}
