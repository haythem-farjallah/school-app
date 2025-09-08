import React from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from 'react-i18next';
import { Building, Users, Calendar, FileText, TrendingUp, Activity } from "lucide-react";
import { 
  DashboardLayout, 
  StatCard, 
  StatGrid, 
  QuickActions, 
  AnnouncementFeed,
} from "@/components/Dashboard";
import { useAuth } from "@/hooks/useAuth";
import { useDashboardStats } from "@/hooks/use-dashboard-stats";
import type { QuickAction } from "@/components/Dashboard";

const StaffDashboard = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: dashboardStats, isLoading: statsLoading } = useDashboardStats();

  // Staff-specific quick actions
  const staffQuickActions: QuickAction[] = [
    {
      id: "manage-enrollments",
      title: "Manage Enrollments",
      description: "Process student enrollments",
      icon: <span className="text-lg">📝</span>,
      variant: "primary",
      onClick: () => navigate("/staff/enrollments"),
    },
    {
      id: "room-management",
      title: "Room Management",
      description: "Schedule and manage rooms",
      icon: <span className="text-lg">🏢</span>,
      variant: "secondary",
      onClick: () => navigate("/staff/rooms"),
    },
    {
      id: "student-records",
      title: "Student Records",
      description: "Access student information",
      icon: <span className="text-lg">👥</span>,
      variant: "success",
      onClick: () => navigate("/staff/students"),
    },
    {
      id: "system-reports",
      title: "System Reports",
      description: "Generate operational reports",
      icon: <span className="text-lg">📊</span>,
      variant: "default",
      onClick: () => navigate("/staff/reports"),
    },
  ];

  // Staff announcements
  const announcements = [
    {
      id: 1,
      title: "New Enrollment Period Opens",
      body: "The enrollment period for the next semester opens tomorrow. Please ensure all systems are ready.",
      importance: "HIGH" as const,
      createdBy: "Administration",
      createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
      isRead: false,
    },
    {
      id: 2,
      title: "System Maintenance Scheduled",
      body: "Scheduled system maintenance this weekend. Please inform users about potential downtime.",
      importance: "MEDIUM" as const,
      createdBy: "IT Department",
      createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // 1 day ago
      isRead: true,
    },
    {
      id: 3,
      title: "Staff Meeting Next Week",
      body: "All staff members are invited to the monthly operational review meeting next Tuesday at 2 PM.",
      importance: "LOW" as const,
      createdBy: "HR Department",
      createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(), // 3 days ago
      isRead: true,
    },
  ];

  // Mock staff stats
  const staffStats = {
    pendingEnrollments: 15,
    activeStudents: dashboardStats?.totalStudents || 450,
    roomUtilization: 78,
    systemHealth: 98,
  };

  return (
    <DashboardLayout
      title={t('Staff Dashboard')}
      subtitle={`Welcome, ${user?.firstName || 'Staff'}! Manage school operations efficiently.`}
      headerIcon={<Building className="h-6 w-6 text-white" />}
      isLoading={statsLoading}
    >
      {/* Stats Overview */}
      <StatGrid columns={4}>
        <StatCard
          title="Pending Enrollments"
          value={staffStats.pendingEnrollments}
          subtitle="Need processing"
          icon={<FileText className="h-4 w-4" />}
          variant="orange"
          isLoading={statsLoading}
          onClick={() => navigate("/staff/enrollments")}
        />
        
        <StatCard
          title="Active Students"
          value={staffStats.activeStudents}
          subtitle="Currently enrolled"
          icon={<Users className="h-4 w-4" />}
          variant="blue"
          isLoading={statsLoading}
          trend={{ value: 12, isPositive: true, label: "vs last month" }}
          onClick={() => navigate("/staff/students")}
        />
        
        <StatCard
          title="Room Utilization"
          value={`${staffStats.roomUtilization}%`}
          subtitle="Current capacity"
          icon={<Building className="h-4 w-4" />}
          variant="green"
          isLoading={statsLoading}
          onClick={() => navigate("/staff/rooms")}
        />
        
        <StatCard
          title="System Health"
          value={`${staffStats.systemHealth}%`}
          subtitle="Operational status"
          icon={<Activity className="h-4 w-4" />}
          variant="purple"
          isLoading={statsLoading}
          trend={{ value: 1, isPositive: true }}
          onClick={() => navigate("/staff/reports")}
        />
      </StatGrid>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Operations Overview */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
            <h3 className="text-xl font-semibold text-gray-900 mb-6">Today's Operations</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 rounded-lg bg-gradient-to-r from-blue-50 to-blue-100 border border-blue-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-medium text-gray-800">New Enrollments</span>
                  <span className="text-sm text-blue-600 font-semibold">Today</span>
                </div>
                <div className="text-2xl font-bold text-blue-600">8</div>
                <p className="text-xs text-blue-500 mt-1">Processed successfully</p>
              </div>
              
              <div className="p-4 rounded-lg bg-gradient-to-r from-green-50 to-green-100 border border-green-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-medium text-gray-800">Room Bookings</span>
                  <span className="text-sm text-green-600 font-semibold">Today</span>
                </div>
                <div className="text-2xl font-bold text-green-600">23</div>
                <p className="text-xs text-green-500 mt-1">Scheduled events</p>
              </div>
              
              <div className="p-4 rounded-lg bg-gradient-to-r from-purple-50 to-purple-100 border border-purple-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-medium text-gray-800">Support Tickets</span>
                  <span className="text-sm text-purple-600 font-semibold">Open</span>
                </div>
                <div className="text-2xl font-bold text-purple-600">3</div>
                <p className="text-xs text-purple-500 mt-1">Awaiting response</p>
              </div>
              
              <div className="p-4 rounded-lg bg-gradient-to-r from-orange-50 to-orange-100 border border-orange-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-medium text-gray-800">Schedule Changes</span>
                  <span className="text-sm text-orange-600 font-semibold">Pending</span>
                </div>
                <div className="text-2xl font-bold text-orange-600">5</div>
                <p className="text-xs text-orange-500 mt-1">Need approval</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">Recent Activities</h3>
            <div className="space-y-3">
              <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                <span className="text-gray-700">Processed enrollment for John Smith (Grade 10)</span>
                <span className="text-xs text-gray-500 ml-auto">2 hours ago</span>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                <span className="text-gray-700">Updated room A-201 schedule for next week</span>
                <span className="text-xs text-gray-500 ml-auto">4 hours ago</span>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
                <span className="text-gray-700">Generated monthly enrollment report</span>
                <span className="text-xs text-gray-500 ml-auto">Yesterday</span>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
                <div className="w-2 h-2 bg-orange-500 rounded-full"></div>
                <span className="text-gray-700">Resolved IT support ticket #1247</span>
                <span className="text-xs text-gray-500 ml-auto">2 days ago</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column - Quick Actions & Announcements */}
        <div className="space-y-6">
          <QuickActions
            actions={staffQuickActions}
            title="Quick Actions"
            columns={1}
          />
          
          <AnnouncementFeed
            announcements={announcements}
            title="Staff Updates"
            maxItems={3}
            onAnnouncementClick={(announcement) => {
              console.log("Clicked announcement:", announcement);
              navigate("/staff/announcements");
            }}
            onViewAll={() => navigate("/staff/announcements")}
          />
        </div>
      </div>

      {/* Additional Operational Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Messages Sent"
          value={142}
          subtitle="This week"
          icon={<FileText className="h-4 w-4" />}
          variant="blue"
          onClick={() => navigate("/staff/messages")}
        />
        
        <StatCard
          title="Reports Generated"
          value={8}
          subtitle="This month"
          icon={<TrendingUp className="h-4 w-4" />}
          variant="green"
          onClick={() => navigate("/staff/reports")}
        />
        
        <StatCard
          title="Schedule Updates"
          value={25}
          subtitle="This week"
          icon={<Calendar className="h-4 w-4" />}
          variant="purple"
          onClick={() => navigate("/staff/schedule")}
        />
        
        <StatCard
          title="Data Backups"
          value="✓ Current"
          subtitle="Last backup: 2hrs ago"
          icon={<Activity className="h-4 w-4" />}
          variant="green"
        />
      </div>
    </DashboardLayout>
  );
};

export default StaffDashboard;
